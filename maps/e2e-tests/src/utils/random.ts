export function getRandomInt(min: number, max: number): number {
  return Math.round(min + Math.random() * (max - min));
}
