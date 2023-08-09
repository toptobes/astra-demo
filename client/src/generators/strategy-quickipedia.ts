import { WikipediaGeneratorStrategy } from "./strategy-wikipedia.ts";
import { GeneratorOutput } from "./generator-pool.ts";
import { toTextBuffer } from "./generator-strategies.ts";

export class QuickipediaGeneratorStrategy extends WikipediaGeneratorStrategy {
  override async refreshBuffer(): Promise<GeneratorOutput[]> {
    return (await this.fetchPages())
      .map(page => [page.title, page.extract, page.pageid])
      .map(([t, e, i]) => {
        return toTextBuffer(e, `en.wikipedia.com/?curid=${i}`, `${t}\n`)
      });
  }
}
