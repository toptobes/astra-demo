import { numInRange, registerSetting, SETTINGS, subscribeToSettingUpdate } from "./settings.ts";
import { errored, success } from "./util.ts";
import { GENERATOR_STRATEGIES } from "./generators/generator-strategies.ts";
import { ColTextGenPool } from "./generators/generator-pool.ts";
import { clearInterval } from "stompjs";

export type ColTextGenerator = AsyncGenerator<[string, boolean], never>

export function setupColumns() {
  setupSettings();
  resetColumns();
}

declare module './settings.ts' {
  interface SettingsRegistry {
    ['num-columns']: number,
    ['parse-speed']: number,
    ['text-gen-fn']: keyof typeof GENERATOR_STRATEGIES,
  }
}

function setupSettings() {
  registerSetting('num-columns', 'NUM_COLUMNS', 2, ...numInRange(0, 8));

  registerSetting('parse-speed', 'PARSE_SPEED', 25, ...numInRange(0, 250));

  registerSetting('text-gen-fn', 'TEXT_GEN_FN', 'wikipedia', (value) => {
    return (!isGeneratorStrategy(value))
      ? errored(`invalid generator (${Object.keys(GENERATOR_STRATEGIES)})`)
      : success(value);
  }, ''+Object.keys(GENERATOR_STRATEGIES));

  subscribeToSettingUpdate([
    'num-columns', 'parse-speed', 'text-gen-fn', 'buffer-add-size', 'buffer-min-size', 'index-by',
  ], resetColumns);
}

let mainLoopID: NodeJS.Timer;

export function stopMainLoop() {
  clearInterval(mainLoopID);
}

function resetColumns() {
  const $COLUMN = (() => {
    const $column = document.createElement('div');
    const $coltxt = document.createElement('div');

    $column.className = "column"
    $coltxt.className = "column-txt"

    $column.append($coltxt);
    return $column;
  })();

  document.querySelector(".columns-container")!.innerHTML = '';

  document.querySelector(".columns-container")!.append(
    ...Array.from({ length: SETTINGS['num-columns'] }).map(_ => $COLUMN.cloneNode(true)) as any[]
  );

  const strategy = GENERATOR_STRATEGIES[SETTINGS['text-gen-fn']];
  const generators = new ColTextGenPool(strategy, SETTINGS['num-columns']).generators;

  const columns = generators.map((generator, i) => {
    return new Column(i, generator);
  });

  let paused = false;

  document.addEventListener('keyup', (e) => {
    if (e.key === 'Escape') paused = !paused;
  });

  mainLoopID = setInterval(async () => {
    if (paused) {
      return;
    }

    for (let column of columns) {
      await column.update();
    }
  }, SETTINGS['parse-speed']);
}

class Column {
  readonly #$column: HTMLDivElement;
  readonly #generator: ColTextGenerator;

  readonly #MAX_SENTENCES = (9 - SETTINGS['num-columns']) * 5;
  readonly #SENTENCES_TO_CULL = this.#MAX_SENTENCES * 2 / 5;

  constructor(id: number, generator: ColTextGenerator) {
    this.#$column = document.querySelectorAll<HTMLDivElement>(`.column-txt`)[id]!;
    this.#generator = generator;
  }

  #setUpNextSentence(): void {
    const lastSentence = this.#$column.lastElementChild! as HTMLSpanElement;

    if (lastSentence) {
      lastSentence.style.opacity = '.9';
      lastSentence.style.filter = '';
    }

    const $span = document.createElement('span');
    $span.style.filter = 'drop-shadow(0 0 .1em white)'

    this.#$column.append($span);
  }

  #cullText(): void {
    if (this.#$column.childElementCount > this.#MAX_SENTENCES) {
      for (let i = 0; i < this.#SENTENCES_TO_CULL; i++) {
        this.#$column.removeChild(this.#$column.firstElementChild!);
      }
    }
  }

  async update(): Promise<void> {
    const [word, isNewSentence] = (await this.#generator.next()).value;

    if (!word) {
      return;
    }

    if (isNewSentence) {
      this.#setUpNextSentence();
    }

    const $sentence = this.#$column.lastElementChild as HTMLSpanElement;

    if (!$sentence) {
      return;
    }
    $sentence.innerText += word;

    this.#$column.scrollTop = this.#$column.scrollHeight;
    this.#cullText();
  }
}

function isGeneratorStrategy(type: string): type is keyof typeof GENERATOR_STRATEGIES {
  // @ts-ignore
  return GENERATOR_STRATEGIES[type] !== undefined;
}

