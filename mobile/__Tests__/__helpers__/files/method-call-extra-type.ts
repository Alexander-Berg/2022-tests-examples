//tslint:disable
const m = new Map<string, string>();
const rm: ReadonlyMap<string, string> = new Map<string, string>();
const s = new Set<string>();
const rs: ReadonlySet<string> = new Set<string>();
const ra: readonly string[] = []
const d = new Date('2018-09-20');
class MyClass {
  method(): number { return 10 }
}
interface MyInterface {
  method(): number;
}
enum MyEnum {
  myValue = "myValue"
}
const c = new MyClass();
const i: MyInterface = { method(): number { return 10 } };
const n = 10;
function f(): ReadonlyArray<ReadonlyArray<string>> {
  return []
}
function identity<T>(value: T): T {
  return value
}
const bi: bigint = BigInt(20)

Number.parseInt('Hello');
n.toFixed();
true.valueOf();
'Hello'.trim();
[1].indexOf(1);
m.has('hello');
s.has('hello');
d.getDay();
Math.abs(1);
c.method();
i.method();
MyEnum.myValue;
ra.indexOf('hello');
rm.has('hello');
rs.has('hello');
f();
identity(true);
identity(1);
identity('foo');
bi.toString();
BigInt.asIntN(24, BigInt(10))
