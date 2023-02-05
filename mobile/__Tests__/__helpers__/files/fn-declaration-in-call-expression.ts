import { undefinedToNull } from '../../../src/ys-tools/ys'

class A { }
const m = new Map<string, string>()

function f(): A {
    return new A()
}
const foo = f

foo()
m.get('hello')
undefinedToNull(m.get('hello'))
