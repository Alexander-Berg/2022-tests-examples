/* eslint-disable */

function f(): void {
  const map: any = new Map<string, string>()
  map.get('key') // fails
}
