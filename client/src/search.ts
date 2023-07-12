import { onSimilarityResult, similarityRequest } from "./stomp/stomp.ts";
import { numInRange, registerSetting, SETTINGS } from "./settings.ts";

const $wrapper = document.querySelector('.cards-container')! as HTMLDivElement;
const $search = document.querySelector('#search-bar')! as HTMLInputElement;

declare module './settings.ts' {
  interface SettingsRegistry {
    ['limit-queries']: number,
  }
}

export function setupSearch() {
  registerSetting('limit-queries', 'LIM_QUERIES', 8, ...numInRange(0, 128));

  $search.onkeyup = (e: KeyboardEvent) => {
    (e.key === 'Enter') && similarityRequest({ query: $search.value, limit: SETTINGS['limit-queries'] });
  };

  onSimilarityResult((results) => {
    const $cards = results.map((result) => {
      return newCard(result.text, result.similarity, result.url);
    });
    $wrapper.replaceChildren(...$cards);
  });
}

function newCard(text: string, similarity: number, url?: string) {
  const $text = document.createElement('p');
  $text.innerText = text;

  const $card = document.createElement('div');
  $card.className = "card";
  $card.style.setProperty('--card-similarity-color', similarity2color(similarity));

  url && ($card.onclick = () => window.open(`https://${url}`, '_blank'))

  $card.appendChild($text);
  return $card;
}

function similarity2color(similarity: number): string {
  return `hsla(${similarity * 60 + 35}, 100%, 40%, .6)`;
}
