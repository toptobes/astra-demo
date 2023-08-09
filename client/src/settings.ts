import { Errorable, errored, success, testDummyWidth } from "./utils.ts";

export function setupSettings() {
  $searchBtn.onclick = () => {
    $container.classList.toggle("expanded");
    $searchBtn.style.rotate = $container.classList.contains("expanded")
      ? '90deg'
      : '0deg';
  };

  document.querySelector<HTMLSpanElement>('#settings-reset-button')!.onclick = () => {
    Object.keys(MUTABLE_SETTINGS).forEach(key => {
      const $input = document.querySelector<HTMLInputElement>(`#settings-${key}`)!;
      $input.value = DEFAULT_SETTINGS[key as keyof SettingsRegistry].toString();
      $input.dispatchEvent(new Event('change'));
    });
  }
}

export interface SettingsRegistry {}

export function registerSetting<
  K extends keyof SettingsRegistry,
  V extends { toString(): string } & SettingsRegistry[K],
>(
  key: K,
  label: string,
  init: V,
  onChange: (value: string) => Errorable<V>,
  allowedValues: string,
) {
  DEFAULT_SETTINGS[key] = init;
  MUTABLE_SETTINGS[key] = getStored(key, init);

  const $wrapper = document.createElement('div');
  const $label = document.createElement('label');
  const $input = document.createElement('input');

  $label.innerText = `${label}=`;

  $input.type = 'search';
  $input.id = `settings-${key}`;
  $input.value = getStored(key, init);

  $input.onchange = () => {
    const rawValue = $input.value;

    if (!rawValue) {
      errorSetting(allowedValues);
    } else {
      const [value, error] = onChange(rawValue);

      (error)
        ? errorSetting(error)
        : applySetting(value);
    }
  };

  let cached = '';

  $input.onfocus = () => {
    errorSetting(`${allowedValues} (current: '${cached = $input.value}')`);
  }

  $input.onblur = () => {
    $input.value || ($input.value = cached);
    setWidthToMatchPlaceholder($input, false);
  }

  $settings[key] = $input;
  $wrapper.append($label, $input);
  document.querySelector('.settings')!.append($wrapper);

  function applySetting(value: any) {
    MUTABLE_SETTINGS[key] = value;
    persist(key, value);
    notifySubscribers(key);
    setWidthToMatchPlaceholder($input, false);
  }

  function errorSetting(error: string) {
    $input.value = '';
    $input.placeholder = error;
    setWidthToMatchPlaceholder($input, true);
  }
}

const $container = document.querySelector<HTMLDivElement>('.settings')!;
const $searchBtn = document.querySelector<HTMLDivElement>('#settings-icon')!;

const DEFAULT_SETTINGS: SettingsRegistry = {} as any;
const MUTABLE_SETTINGS: SettingsRegistry = {} as any;

const $settings: Record<string, HTMLDivElement> = {}
const subscribers: Record<string, (() => void)[]> = {}

export const SETTINGS = MUTABLE_SETTINGS as Readonly<typeof DEFAULT_SETTINGS>;

export function subscribeToSettingUpdate(settings: (keyof SettingsRegistry)[], callback: () => void) {
  settings.forEach(setting => {
    if (!$settings[setting]) {
      throw new Error(`${setting} is not a valid setting`);
    }
    subscribers[setting] = subscribers[setting] ?? [];
    subscribers[setting].push(callback);
  });
}

function notifySubscribers(setting: string) {
  subscribers[setting]?.forEach(callback => callback());
}

function setWidthToMatchPlaceholder($input: HTMLInputElement, match: boolean) {
  if (match) {
    const width = testDummyWidth('span', $dummy => {
      $dummy.innerText = $input.placeholder;
    });
    $input.style.width = `calc(${width}px + 0.2em)`;
  } else {
    const width = testDummyWidth('input', $dummy => {
      $dummy.style.fontSize = '.975em';
    });
    $input.style.width = width + 'px';
  }
}

const numInRangeChecker = (min: number, max: number) => (value: string): Errorable<number> => {
  const number = parseInt(value);

  return (number < min || max < number || isNaN(number))
    ? errored(`number out of bounds [${min}, ${max}]`)
    : success(number);
}

export const numInRange = (min: number, max: number) => {
  return [numInRangeChecker(min, max), `[${min}, ${max}]`] as const;
};

function persist(key: string, value: any) {
  try {
    localStorage.setItem(key, JSON.stringify({ value }));
  } catch (e) {
    localStorage.setItem(key, JSON.stringify({ value: `"${value}"`}));
  }
}

function getStored(key: string, init: any): any {
  const value = localStorage.getItem(key);
  return value ? JSON.parse(value).value : init;
}
