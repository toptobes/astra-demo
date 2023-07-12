import { WikipediaGeneratorStrategy } from "./strategy-wikipedia.ts";
import { QuickipediaGeneratorStrategy } from "./strategy-quickipedia.ts";
import { LoremGeneratorStrategy } from "./strategy-lorem.ts";
import { SETTINGS } from "../settings.ts";
import { GeneratorOutput } from "./generator-pool.ts";

export interface ColumnTextGenerationStrategy {
  refreshBuffer(): Promise<GeneratorOutput[]>;
}

export const GENERATOR_STRATEGIES = {
  wikipedia: WikipediaGeneratorStrategy,
  quickipedia: QuickipediaGeneratorStrategy,
  lorem: LoremGeneratorStrategy,
};

export function toTextBuffer(toIndex: string, url?: string, toDisplay: string = toIndex): GeneratorOutput {
  const sentences = toDisplay.split(/(?<=[.!?])/g);

  const text2index =
    (SETTINGS['index-by'] === 'sentence' && Object.is(toIndex, toDisplay))
      ? sentences :
      (SETTINGS['index-by'] === 'sentence')
        ? toIndex.split(/(?<=[.!?])/g)
        : [toIndex];

  const indexBatch = text2index.map(text => ({
    text: text,
    url: url
  }));

  const wordsBatch = sentences.map(s => s.split(/(?= )/g));
  return [indexBatch, wordsBatch];
}
