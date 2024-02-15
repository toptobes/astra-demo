import { SETTINGS } from '../settings.ts';
import { ColumnTextGenerationStrategy, toTextBuffer } from './generator-strategies.ts';
import { GeneratorOutput } from './generator-pool.ts';

export class CatpediaGeneratorStrategy implements ColumnTextGenerationStrategy {
  static #API_URL = 'https://en.wikipedia.org/w/api.php?action=query&format=json&origin=*&generator=random&grnnamespace=0&prop=extracts&exintro&explaintext&grnlimit=';
  static readonly #EX_LIMIT = 20;
  static readonly #MIN_WORDS = 300;

  async fetchPage(limit: number): Promise<any[]> {
    try {
      const response = await fetch(CatpediaGeneratorStrategy.#API_URL + limit);
      const body = await response.json();

      return Object.values(body.query.pages);
    } catch (e) {
      console.error(e);
      return [];
    }
  }

  async fetchPages(): Promise<any[]> {
    const results: Promise<any>[] = [];
    let completed = 0;

    while (completed < SETTINGS['buffer-add-size']) {
      const limit = Math.min(CatpediaGeneratorStrategy.#EX_LIMIT, SETTINGS['buffer-add-size'] - completed);
      completed += limit;

      const pages = this.fetchPage(limit);
      results.push(pages);
    }

    return Promise.all(results).then(arr => arr.flat());
  }

  async refreshBuffer(): Promise<GeneratorOutput[]> {
    return concat(await this.fetchPages(), CatpediaGeneratorStrategy.#MIN_WORDS)
      .map(([e, t]) => toTextBuffer(e, '', `${t}\n\n`));
  }
}

const concat = (pages: any[], minWords: number): [string, string][] => {
  const buffers: string[] = [''];
  const titles: string[][] = [[]];
  let i = 0;

  for (const page of pages) {
    buffers[i] += page.extract;
    titles[i].push(page.title);

    if (countWords(buffers[i]) > minWords) {
      buffers.push('');
      titles.push([]);
      i++;
    }
  }

  return buffers.map((buffer, i) => [buffer, titles[i].join(', ')]);
}

const countWords = (text: string): number => {
  return text.split(/\s+/).length;
}
