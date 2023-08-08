import { SETTINGS } from "../settings.ts";
import { ColumnTextGenerationStrategy, toTextBuffer } from "./generator-strategies.ts";
import { GeneratorOutput } from "./generator-pool.ts";

export class WikipediaGeneratorStrategy implements ColumnTextGenerationStrategy {
  static #API_URL = 'https://en.wikipedia.org/w/api.php?action=query&format=json&origin=*&generator=random&grnnamespace=0&prop=extracts&exintro&explaintext&grnlimit=';
  static readonly #EX_LIMIT = 20;

  async fetchPage(limit: number): Promise<any[]> {
    try {
      const response = await fetch(WikipediaGeneratorStrategy.#API_URL + limit);
      const body = await response.json();

      return Object.values(body.query.pages);
    } catch (e) {
      console.log(e);
      return [];
    }
  }

  async fetchPages(): Promise<any[]> {
    const results: Promise<any>[] = [];
    let completed = 0;

    while (completed < SETTINGS['buffer-add-size']) {
      const limit = Math.min(WikipediaGeneratorStrategy.#EX_LIMIT, SETTINGS['buffer-add-size'] - completed);
      completed += limit;

      const pages = this.fetchPage(limit);
      results.push(pages);
    }

    return Promise.all(results).then(arr => arr.flat());
  }

  async refreshBuffer(): Promise<GeneratorOutput[]> {
    return (await this.fetchPages())
      .map(page => [page.extract, page.pageid])
      .map(([e, i]) => toTextBuffer(e, `en.wikipedia.com/?curid=${i}`))
  }
}
