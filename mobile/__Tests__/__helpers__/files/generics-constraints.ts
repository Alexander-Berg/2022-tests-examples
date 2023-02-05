//tslint:disable
interface I1 { }
interface I2 { }
class C1 { }
class C2 { }
class C3<T> { }
type Z = C1
function C4(): void { }

class A1<T extends I1> { }
class A2<T extends I1 & I2> { }
class A3<T extends C1> { }
class A4<T extends C1 & I1> { }
class A5<T extends C1 & I1 & I2> { }
class A6<T extends C1, U extends C1 & I1> { }
class A7<T extends C3<number>> { }

function f1<T extends I1>(): void { }
function f2<T extends I1 & I2>(): void { }
function f3<T extends C1>(): void { }
function f4<T extends C1 & I1>(): void { }
function f5<T extends C1 & I1 & I2>(): void { }
function f6<T extends C1, U extends C1 & I1>(): void { }
function f7<T extends C3<number>>(): void { }

type T1<T extends I1> = void
type T2<T extends I1 & I2> = void
type T3<T extends C1> = void
type T4<T extends C1 & I1> = void
type T5<T extends C1 & I1 & I2> = void
type T6<T extends C1, U extends C1 & I1> = void
type T7<T extends C3<number>> = void

class A0<T> { }
function f0<T>(): void { }
type T0<T> = void

class A10<T extends Number> { }
function f10<T extends String>(): void { }
type T10<T extends Function> = void

class A11<T extends C1 & Number> { }
function f11<T extends C1 & String>(): void { }
type T11<T extends C1 & Function> = void

class A12<T extends I1 | I2> { }
function f13<T extends I1 | I2>(): void { }
type T14<T extends I1 | I2> = void
