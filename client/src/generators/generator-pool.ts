import { ColTextGenerator } from "../columns.ts";
import { numInRange, registerSetting, SETTINGS } from "../settings.ts";
import { errored, success } from "../util.ts";
import { ColumnTextGenerationStrategy, GENERATOR_STRATEGIES } from "./generator-strategies.ts";
import { IndexRequest } from "../stomp/messages.ts";
import { indexRequest } from "../stomp/stomp.ts";

declare module '../settings.ts' {
  interface SettingsRegistry {
    ['buffer-add-size']: number,
    ['buffer-min-size']: number,
    ['index-by']: 'sentence' | 'page',
  }
}

registerSetting('buffer-min-size', 'BUF_MIN_SIZ', 40, ...numInRange(1, 500));

registerSetting('buffer-add-size', 'BUF_ADD_SIZ', 40, ...numInRange(1, 500));

registerSetting('index-by', 'INDEXING_BY', 'page', (value) => {
  return (value !== 'sentence' && value !== 'page')
    ? errored(`invalid generator (${Object.keys(GENERATOR_STRATEGIES)})`)
    : success(value);
}, 'sentence, page');

type TextBuffer = string[][];
export type GeneratorOutput = [IndexRequest[], TextBuffer];

export class ColTextGenPool {
  readonly #buffer: TextBuffer[] = [];
  #isRefreshing = false;

  readonly #states: {
    sentenceIdx: number;
    wordIdx: number;
    text: string[][];
  }[];

  readonly generators: ColTextGenerator[];

  readonly #BUFFERING_MSG = [["...\n"]];
  readonly #MAX_BUFFERING_MSGS = 3;

  constructor(strategyType: new () => ColumnTextGenerationStrategy, numColumns: number) {
    this.#states = Array.from({ length: numColumns }).map(_ => ({
      sentenceIdx: 0,
      wordIdx: 0,
      text: [],
    }));

    const strategy = new strategyType();
    this.#refreshBuffer(strategy);

    this.generators = this.#states.map(({ sentenceIdx, wordIdx, text }) => {
      let numBufferingMsgs = 0;

      const fn = async function * (this: ColTextGenPool): ColTextGenerator {
        while (true) {
          if (text.length === 0) {
            text = this.#buffer.pop() || this.#BUFFERING_MSG;

            if (Object.is(text, this.#BUFFERING_MSG)) {
              numBufferingMsgs++;

              if (numBufferingMsgs > this.#MAX_BUFFERING_MSGS) {
                yield ['', false];
                text = [];
                continue;
              }
            } else {
              numBufferingMsgs = 0;
            }

            if (this.#buffer.length <= SETTINGS['buffer-min-size']) {
              this.#refreshBuffer(strategy);
            }

            if (!this.#buffer.length) {
              console.log("Needs refresh")
            }
          }

          let isNewSentence = wordIdx === 0;
          yield [text[sentenceIdx][wordIdx++], isNewSentence];

          if (text[sentenceIdx][wordIdx] === undefined) {
            sentenceIdx++;
            wordIdx = 0;
          }

          if (text[sentenceIdx] === undefined) {
            text = [];
            sentenceIdx = 0;
            wordIdx = 0;
          }
        }
      };

      return fn.bind(this)();
    });
  }

  #refreshBuffer(strategy: ColumnTextGenerationStrategy) {
    if (this.#isRefreshing) {
      return;
    }

    this.#isRefreshing = true;

    console.log("Refreshing buffer")

    strategy.refreshBuffer().then(newBuffers => {
      console.log("Refreshed")

      const batchesFlattened = newBuffers.flatMap(buffer => buffer[0]);
      indexRequest(batchesFlattened);

      const words = newBuffers.map(buffer => buffer[1])
      this.#buffer.push(...words);

      this.#isRefreshing = false;
    });
  }
}
