//tslint:disable
import { f } from './has-nullable-method'
import { Nullable } from '../../../src/ys-tools/ys'
function getNull(n: number): Nullable<number> {
  return (n % 2 === 0) ? null : n
}
class A {
  static b: Nullable<string> = 'Hey'
}
const typedN: Nullable<number> = getNull(4)
const untypedN = getNull(3)
const typedUN: number = 4
const untypedUN = 3
typedN?.toString()
untypedN.toFixed()
typedUN.toString()
untypedUN.toFixed()
getNull(3)
A.b
f((r) => {
  const n = r.getError().getInner()
})
if (typedN !== null) {
  typedN.toString()
}
