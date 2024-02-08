import { onSimilarityResult, similarityRequest } from "./stomp/stomp.ts";
import { numInRange, registerSetting, SETTINGS } from "./settings.ts";

const $wrapperDense = document.querySelector('.cards-container-dense')! as HTMLDivElement;
const $wrapperMulti = document.querySelector('.cards-container-multi')! as HTMLDivElement;
const $search = document.querySelector('#search-bar')! as HTMLInputElement;

declare module './settings.ts' {
  interface SettingsRegistry {
    ['limit-queries']: number,
  }
}

export function setupSearch() {
  registerSetting('limit-queries', 'LIM_QUERIES', 12, ...numInRange(0, 128));

  $search.onkeyup = (e: KeyboardEvent) => {
    (e.key === 'Enter') && similarityRequest({ query: $search.value, limit: SETTINGS['limit-queries'] });
  };

  onSimilarityResult((results) => {
    const $cardsDense = results.dense.map((result) => {
      return newCard(result.text, 0.0, result.url);
    });
    $wrapperDense.replaceChildren('Dense', ...$cardsDense);

    const $cardsMulti = results.multi.map((result) => {
      return newCard(result.text, 1.0, result.url);
    });
    $wrapperMulti.replaceChildren('Multi-vector', ...$cardsMulti);
  });
}

function newCard(text: string, similarity: number, url?: string) {
  const $text = document.createElement('p');
  $text.innerText = text;

  const $card = document.createElement('div');
  $card.className = "card";
  $card.style.setProperty('--card-similarity-color', similarity2color(similarity));

  url && ($card.onclick = () => window.open(`https://${url}`, '_blank'))

  $card.title = 'Cosine similarity: ' + similarity;

  $card.appendChild($text);
  return $card;
}

function similarity2color(similarity: number): string {
  return `hsla(${(rescale(similarity, [0.7, 0.8], [0, 1]) ** 1.2) * 120}, 100%, 40%, .6)`;
}

function rescale(input: number, inputRange: [number, number], outputRange: [number, number]) {
  let output = (input - inputRange[0]) * (outputRange[1] - outputRange[0]) / (inputRange[1] - inputRange[0]) + outputRange[0];

  if (output < outputRange[0]) {
    output = outputRange[0];
  } else if (output > outputRange[1]) {
    output = outputRange[1];
  }

  return output;
}
