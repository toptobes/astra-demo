export type Errorable<T> = [T, undefined] | [undefined, string];

export function success<T>(value: T): Errorable<T> {
  return [value, undefined];
}

export function errored<T>(error: string): Errorable<T> {
  return [undefined, error];
}

export function testDummyWidth<K extends keyof HTMLElementTagNameMap>(key: K, config?: (e: HTMLElementTagNameMap[K]) => void): number {
  const $dummy = document.createElement(key);
  $dummy.style.visibility = 'hidden';
  $dummy.style.position = 'absolute';
  config?.($dummy);

  document.body.appendChild($dummy);
  const width = $dummy.offsetWidth;
  document.body.removeChild($dummy);
  return width;
}
