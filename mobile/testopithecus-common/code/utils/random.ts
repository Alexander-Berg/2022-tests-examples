import { Int32 } from '../../../../common/ys'

export interface RandomProvider {
  generate(n: Int32): Int32
}

export function randomInt(random: RandomProvider, min: Int32, max: Int32): Int32 {
  return random.generate(max - min) + min
}

export function randomInterval(random: RandomProvider, min: Int32, max: Int32): Int32[] {
  const first = randomInt(random, min, max)
  const second = randomInt(random, min, max)
  if (first > second) {
    return [second, first]
  } else {
    return [first, second]
  }
}
