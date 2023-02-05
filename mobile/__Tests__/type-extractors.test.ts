import FileProcessor from '../src/fileprocessor'
import { TSVisibility } from '../src/generators-model/basic-types'
import {
  TSBinaryExpression,
  TSCallExpression,
  TSExpressionKind,
  TSIdentifier,
  TSMemberAccessExpression,
  TSObjectCreationExpression,
} from '../src/generators-model/expression'
import { TestGenerator } from './__helpers__/generator'
import {
  alias_,
  any_,
  anyArg,
  arr_,
  arrArg,
  block_,
  bool_,
  boolArg,
  class_,
  constructor_,
  ctorArg,
  double_,
  doubleArg,
  e_empty,
  e_extends,
  e_full,
  e_implements,
  enum_,
  enumWithValues_,
  field_,
  field_extra_,
  fun_,
  funArg,
  function_,
  gen_,
  int32_,
  int32Arg,
  int64_,
  int64Arg,
  interface_,
  lit_,
  m_,
  makeSourceFile,
  method_,
  null_,
  nullArg,
  ref_,
  refArg,
  refs_,
  str_,
  strArg,
  void_,
} from './__helpers__/test-helpers'

describe('Type Extractors', () => {
  // INTERFACES
  it('should extract interfaces from program text', () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = `
    interface I1 {}
    export interface I2 extends I1<string, I1> {}
    export interface I3 extends I1, I2 {}
    `
    processor.process(makeSourceFile(contents))
    expect(generator.interfaces[0]).toMatchObject(interface_('I1', [], [], false, []))
    expect(generator.interfaces[1]).toMatchObject(interface_('I2', [ref_('I1', [str_(), ref_('I1')])], [], true, []))
    expect(generator.interfaces[2]).toMatchObject(interface_('I3', refs_('I1', 'I2'), [], true, []))
  })
  it('should extract methods from interface', () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = `
    export interface I1 {
      method1(a: string, b: number, c: boolean, d: any): void
      method2(): string
      method3(a: string): number
      method4(a: A): B
    }
    `
    processor.process(makeSourceFile(contents))
    const expected = [
      method_(
        'method1',
        TSVisibility.Public,
        [strArg('a'), int32Arg('b'), boolArg('c'), anyArg('d')],
        void_(),
        m_(false, false, false, false),
        [],
      ),
      method_('method2', TSVisibility.Public, [], str_(), m_(false, false, false, false), []),
      method_('method3', TSVisibility.Public, [strArg('a')], int32_(), m_(false, false, false, false), []),
      method_('method4', TSVisibility.Public, [refArg('a', 'A')], ref_('B'), m_(false, false, false, false), []),
    ]

    expect(generator.interfaces[0]).toMatchObject(interface_('I1', [], expected, true, []))
  })
  it('should throw if generics are used in interfaces', () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = `
    interface I<T1, T2> {
      method1(a: T1): T2
      method2<C>(a: T1): C
    }
    `
    expect(() => {
      processor.process(makeSourceFile(contents))
    }).toThrowError('Type parameters <Generics> are not supported for interfaces')
  })
  it("should support arrays in interfaces' methods", () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = `
    interface A {
      m1(a: Int32[]): boolean[]
      m2<T, R>(a: readonly T[]): ReadonlyArray<R[]>
    }
    `
    processor.process(makeSourceFile(contents))
    const expectedMethods = [
      method_(
        'm1',
        TSVisibility.Public,
        [arrArg('a', int32_(), false)],
        arr_(bool_(), false),
        m_(false, false, false, false),
        [],
      ),
      method_(
        'm2',
        TSVisibility.Public,
        [arrArg('a', ref_('T'), true)],
        arr_(arr_(ref_('R'), false), true),
        m_(false, false, false, false),
        [gen_('T'), gen_('R')],
      ),
    ]
    expect(generator.interfaces[0].methods).toMatchObject(expectedMethods)
  })
  it('should throw if an interface has methods with untyped parameters', () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = `
    interface A {
      m(a, b: number): string
    }
    `
    expect(() => {
      processor.process(makeSourceFile(contents))
    }).toThrowError(/Untyped function parameters are not supported/)
  })
  it('should throw if an interface has methods with unspecified return type', () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = `
    interface A {
      m(a: string, b: number)
    }
    `
    expect(() => {
      processor.process(makeSourceFile(contents))
    }).toThrowError(/Methods with unspecified return types are not supported/)
  })
  it('should throw if an interface has fields with initializers', () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = `
    interface A {
      a: number = 10
    }
    `
    expect(() => {
      processor.process(makeSourceFile(contents))
    }).toThrowError('Instance properties with initializers in interfaces are not supported')
  })

  // CLASSES
  it('should extract classes from program text', () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = `
    export class C1 {}
    class C2 extends C1 {}
    export class C3 extends C1 implements I1, I2 {}
    class C4 implements I1, I2 {}
    `
    processor.process(makeSourceFile(contents))
    expect(generator.classes[0]).toMatchObject(
      class_('C1', e_empty(), undefined, [], [], false, true, [], false, false),
    )
    expect(generator.classes[1]).toMatchObject(
      class_('C2', e_extends('C1'), undefined, [], [], false, false, [], false, false),
    )
    expect(generator.classes[2]).toMatchObject(
      class_('C3', e_full('C1', ['I1', 'I2']), undefined, [], [], false, true, [], false, false),
    )
    expect(generator.classes[3]).toMatchObject(
      class_('C4', e_implements(['I1', 'I2']), undefined, [], [], false, false, [], false, false),
    )
  })
  it('should throw on anonymous class', () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = `
    class {
      constructor(a: number, b: string)
    }
    `
    expect(() => {
      processor.process(makeSourceFile(contents))
    }).toThrowError(/Anonymous classes are not supported/)
  })
  it('should extract constructors from classes', () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = `
    class A {
      public constructor(a: number, b: string) { }
    }
    `
    processor.process(makeSourceFile(contents))
    const classes = generator.classes
    expect(classes).toHaveLength(1)
    expect(classes[0].ctor).toMatchObject(
      constructor_([ctorArg('a', int32_()), ctorArg('b', str_())], TSVisibility.Public, false, block_()),
    )
  })
  it('should extract constructors with fields declaration from classes', () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = `
    class A {
      public constructor(private a: number = 10, readonly b: string, protected readonly c: boolean, d: Int64) { }
    }
    `
    processor.process(makeSourceFile(contents))
    const classes = generator.classes
    expect(classes).toHaveLength(1)
    expect(classes[0].ctor).toMatchObject(
      constructor_(
        [
          ctorArg('a', int32_(), false, false, false, TSVisibility.Private, lit_(10)),
          ctorArg('b', str_(), true, false, false, TSVisibility.Public),
          ctorArg('c', bool_(), true, false, false, TSVisibility.Protected),
          ctorArg('d', int64_()),
        ],
        TSVisibility.Public,
        false,
        block_(),
      ),
    )
  })
  it("should throw if constructors' parameters are without types", () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = `
    class A {
      public constructor(private a) { }
    }
    `
    expect(() => processor.process(makeSourceFile(contents))).toThrowError(
      /Untyped constructor parameters are not supported/,
    )
  })
  it('should throw if there are unsupported modifiers in fields in constructor', () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = `
    class A {
      public constructor(static a: number) { }
    }
    `
    expect(() => processor.process(makeSourceFile(contents))).toThrowError(
      /Unsupported type of constructor parameter modifier/,
    )
  })
  it('should throw on bodiless constructor', () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = `
    class A {
      constructor(a: number, b: string)
    }
    `
    expect(() => {
      processor.process(makeSourceFile(contents))
    }).toThrowError(/Constructors with no bodies are not supported/)
  })
  it('should extract methods from classes', () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = `
    abstract class A {
      static method1(a: string, b: Int32, c: boolean, d: any): void {}
      abstract method2(): string
      method3(a: string): Int64 {}
      method4(a: A): A {}
    }
    `
    processor.process(makeSourceFile(contents))

    const expected = [
      method_(
        'method1',
        TSVisibility.Public,
        [strArg('a'), int32Arg('b'), boolArg('c'), anyArg('d')],
        void_(),
        m_(false, true, false, false),
        [],
        block_(),
      ),
      method_('method2', TSVisibility.Public, [], str_(), m_(true, false, false, false), []),
      method_('method3', TSVisibility.Public, [strArg('a')], int64_(), m_(false, false, false, false), [], block_()),
      method_(
        'method4',
        TSVisibility.Public,
        [refArg('a', 'A')],
        ref_('A'),
        m_(false, false, false, false),
        [],
        block_(),
      ),
    ]

    expect(generator.classes).toHaveLength(1)
    expect(generator.classes[0].isAbstract).toBeTruthy()
    expect(generator.classes[0].methods).toMatchObject(expected)
  })
  it('should extract constructor and methods from classes', () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = `
    abstract class A {
      private constructor(a: Double, b: string) { }
      abstract method1(a: string, b: Double, c: boolean, d: any): void {}
      static method2(): string {}
      method3(a: string): Int64 {}
      method4(a: A): A {}
    }
    `
    processor.process(makeSourceFile(contents))

    const expected = [
      method_(
        'method1',
        TSVisibility.Public,
        [strArg('a'), doubleArg('b'), boolArg('c'), anyArg('d')],
        void_(),
        m_(true, false, false, false),
        [],
        block_(),
      ),
      method_('method2', TSVisibility.Public, [], str_(), m_(false, true, false, false), [], block_()),
      method_('method3', TSVisibility.Public, [strArg('a')], int64_(), m_(false, false, false, false), [], block_()),
      method_(
        'method4',
        TSVisibility.Public,
        [refArg('a', 'A')],
        ref_('A'),
        m_(false, false, false, false),
        [],
        block_(),
      ),
    ]

    expect(generator.classes).toHaveLength(1)
    expect(generator.classes[0].isAbstract).toBeTruthy()
    expect(generator.classes[0].ctor).toMatchObject(
      constructor_([ctorArg('a', double_()), ctorArg('b', str_())], TSVisibility.Private, false, block_()),
    )
    expect(generator.classes[0].methods).toMatchObject(expected)
  })
  it('should extract fields from classes', () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = `
    class A {
      @JsonProperty('aa')
      readonly a: number

      @JsonProperty('bb')
      b: boolean

      @weak c: Nullable<T>
    }
    `
    processor.process(makeSourceFile(contents))
    const expected = [
      field_('a', int32_(), TSVisibility.Public, field_extra_(true, false, false, false, false, 'aa')),
      field_('b', bool_(), TSVisibility.Public, field_extra_(false, false, false, false, false, 'bb')),
      field_('c', null_(ref_('T')), TSVisibility.Public, field_extra_(false, false, true, false, false)),
    ]
    expect(generator.classes[0].fields).toMatchObject(expected)
  })
  it('should throw for unsupported property annotations', () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = `
    class A {
      @JsonProperty(10)
      a: boolean
    }
    `
    expect(() => processor.process(makeSourceFile(contents))).toThrowError(
      'Only text literal values are supported as parameters to property annotations',
    )
  })
  it('should throw if weak field is of non-nullable type', () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = `
    class A {
      @weak c: T
    }
    `
    expect(() => {
      processor.process(makeSourceFile(contents))
    }).toThrowError('Weak fields must be nullable reference types')
  })
  it('should throw if weak field is of non-reference type', () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = `
    class A {
      @weak c: Nullable<string>
    }
    `
    expect(() => {
      processor.process(makeSourceFile(contents))
    }).toThrowError('Weak fields must be nullable reference types')
  })
  it('should extract fields with initializers from classes', () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = `
    class A {
      readonly a: Int32 = 10
      d: boolean = f()
    }
    `
    processor.process(makeSourceFile(contents))
    const expected = [
      field_('a', int32_(), TSVisibility.Public, field_extra_(true, false, false, false, false), lit_(10)),
      field_('d', bool_(), TSVisibility.Public, field_extra_(false, false, false, false, false), {
        kind: TSExpressionKind.Call,
        expression: {
          kind: TSExpressionKind.Identifier,
          name: 'f',
        },
        args: [],
        typeArguments: [],
        optionalChaining: false,
      } as TSCallExpression),
    ]
    expect(generator.classes[0].fields).toMatchObject(expected)
  })
  it('should extract visibility qualifiers from classes', () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = `
    class A {
      public a: number
      protected b: string
      private static readonly c: Int32 = 10
      private static readonly d: bigint = 10

      public method1(a: Int64): void
      protected static method2(b: string): any
      private method3(): boolean
    }
    `
    processor.process(makeSourceFile(contents))
    const expectedFields = [
      field_('a', int32_(), TSVisibility.Public, field_extra_(false, false, false, false, false)),
      field_('b', str_(), TSVisibility.Protected, field_extra_(false, false, false, false, false)),
      field_('c', int32_(), TSVisibility.Private, field_extra_(true, true, false, false, false), lit_(10)),
      field_('d', int64_(), TSVisibility.Private, field_extra_(true, true, false, false, false), lit_(10)),
    ]
    const expectedMethods = [
      method_('method1', TSVisibility.Public, [int64Arg('a')], void_(), m_(false, false, false, false), []),
      method_('method2', TSVisibility.Protected, [strArg('b')], any_(), m_(false, true, false, false), []),
      method_('method3', TSVisibility.Private, [], bool_(), m_(false, false, false, false), []),
    ]
    expect(generator.classes[0].fields).toMatchObject(expectedFields)
    expect(generator.classes[0].methods).toMatchObject(expectedMethods)
  })
  it('should throw if readonly static fields do not have initializer', () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = `
    class A {
      private static readonly c: number
    }
    `
    expect(() => {
      processor.process(makeSourceFile(contents))
    }).toThrowError(/Static fields must have initializers/)
  })
  it('should throw if readwrite static fields do not have initializer', () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = `
    class A {
      private static c: number
    }
    `
    expect(() => {
      processor.process(makeSourceFile(contents))
    }).toThrowError(/Static fields must have initializers/)
  })
  it('should support static fields with initializers in classes', () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = `
    class A {
      private static a: Int64 = 10
      public readonly static b: string = 'Hello'
      public static c: Double = 10 + B.f()
    }
    `
    const expectedFields = [
      field_('a', int64_(), TSVisibility.Private, field_extra_(false, true, false, false, false), lit_(10)),
      field_('b', str_(), TSVisibility.Public, field_extra_(true, true, false, false, false), lit_('Hello')),
      field_('c', double_(), TSVisibility.Public, field_extra_(false, true, false, false, false), {
        kind: TSExpressionKind.Binary,
        operator: '+',
        left: {
          kind: TSExpressionKind.Literal,
          value: 10,
        },
        right: {
          kind: TSExpressionKind.Call,
          expression: {
            kind: TSExpressionKind.MemberAccess,
            object: {
              kind: TSExpressionKind.Identifier,
              name: 'B',
            },
            member: {
              kind: TSExpressionKind.Identifier,
              name: 'f',
            },
          },
          args: [],
          typeArguments: [],
        },
      } as TSBinaryExpression),
    ]
    processor.process(makeSourceFile(contents))
    expect(generator.classes[0].fields).toMatchObject(expectedFields)
  })
  it('should support generics in classes', () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = `
    class A<T1, T2> {
      a: T1

      method1(a: T1): T2
      method2<C>(a: T1): C
    }
    `
    processor.process(makeSourceFile(contents))
    const expectedFields = [
      field_('a', ref_('T1'), TSVisibility.Public, field_extra_(false, false, false, false, false)),
    ]
    const expectedMethods = [
      method_('method1', TSVisibility.Public, [refArg('a', 'T1')], ref_('T2'), m_(false, false, false, false), []),
      method_('method2', TSVisibility.Public, [refArg('a', 'T1')], ref_('C'), m_(false, false, false, false), [
        gen_('C'),
      ]),
    ]
    expect(generator.classes[0].generics).toMatchObject([gen_('T1'), gen_('T2')])
    expect(generator.classes[0].fields).toMatchObject(expectedFields)
    expect(generator.classes[0].methods).toMatchObject(expectedMethods)
  })
  it("should support arrays in classes' fields and methods", () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = `
    class A<Z> {
      v1: readonly string[]
      v2: Z[]
      v3: Array<Z>
      v4: ReadonlyArray<readonly Z[]>

      m1(a: readonly Int64[]): readonly boolean[][]
      m2<T, R>(a: T[]): R[]
    }
    `
    processor.process(makeSourceFile(contents))
    const expectedFields = [
      field_('v1', arr_(str_(), true), TSVisibility.Public, field_extra_(false, false, false, false, false)),
      field_('v2', arr_(ref_('Z'), false), TSVisibility.Public, field_extra_(false, false, false, false, false)),
      field_('v3', arr_(ref_('Z'), false), TSVisibility.Public, field_extra_(false, false, false, false, false)),
      field_(
        'v4',
        arr_(arr_(ref_('Z'), true), true),
        TSVisibility.Public,
        field_extra_(false, false, false, false, false),
      ),
    ]
    const expectedMethods = [
      method_(
        'm1',
        TSVisibility.Public,
        [arrArg('a', int64_(), true)],
        arr_(arr_(bool_(), false), true),
        m_(false, false, false, false),
        [],
      ),
      method_(
        'm2',
        TSVisibility.Public,
        [arrArg('a', ref_('T'), false)],
        arr_(ref_('R'), false),
        m_(false, false, false, false),
        [gen_('T'), gen_('R')],
      ),
    ]
    expect(generator.classes[0].generics).toMatchObject([gen_('Z')])
    expect(generator.classes[0].fields).toMatchObject(expectedFields)
    expect(generator.classes[0].methods).toMatchObject(expectedMethods)
  })
  it('should throw if a class has untyped fields', () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = `
    class A {
      public v
    }
    `
    expect(() => {
      processor.process(makeSourceFile(contents))
    }).toThrowError(/Fields with unspecified types are not supported/)
  })
  it('should throw if a class has methods with untyped parameters', () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = `
    class A {
      m(a, b: number): string { return '' }
    }
    `
    expect(() => {
      processor.process(makeSourceFile(contents))
    }).toThrowError(/Untyped function parameters are not supported/)
  })
  it('should throw if a class has methods with unspecified return type', () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = `
    class A {
      m(a: string, b: number) { return '' }
    }
    `
    expect(() => {
      processor.process(makeSourceFile(contents))
    }).toThrowError(/Methods with unspecified return types are not supported/)
  })
  it('should mark class as serializable with annotation', () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = `
    @Serializable()
    export class A {
    }
    `
    processor.process(makeSourceFile(contents))
    expect(generator.classes[0].extra.hasSerializableAnnotatiton).toBe(true)
  })
  it('should not mark class as serializable without annotation', () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = `
    export class A {
    }
    `
    processor.process(makeSourceFile(contents))
    expect(generator.classes[0].extra.hasSerializableAnnotatiton).toBe(false)
  })

  // IMPORTS
  it('should extract imported types', () => {
    function pair(name: string, source: string): { name: string; source: string } {
      return {
        name,
        source,
      }
    }

    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = `
    import {A, B} from './source/A_B'
    import { C } from 'C'
    `
    processor.process(makeSourceFile(contents))
    const pairs = generator.imports.getAllPairs()
    expect(pairs).toMatchObject([pair('A', './source/A_B'), pair('B', './source/A_B'), pair('C', 'C')])
  })
  it('should throw if namespaced import type found', () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = `
    import A from './source/A_B'
    `
    expect(() => {
      processor.process(makeSourceFile(contents))
    }).toThrowError(/Namespaced import not supported: file 'test\.ts'/)
  })
  it('should throw if unsupported type of import', () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = `
    const s = './source/A_B'
    import A from s
    `
    expect(() => {
      processor.process(makeSourceFile(contents))
    }).toThrowError(/Unsupported import clause: file 'test\.ts'/)
  })

  // ALIASES
  it('should extract aliases', () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = `
    export type A = Int32
    type B = B1
    export type C = B
    type D = (arg: Int64) => string
    `
    processor.process(makeSourceFile(contents))
    const expected = [
      alias_('A', true, int32_()),
      alias_('B', false, ref_('B1')),
      alias_('C', true, ref_('B')),
      alias_('D', false, fun_([int64_()], str_())),
    ]
    expect(generator.aliases).toMatchObject(expected)
  })
  it('should support generics in type aliases', () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = `
    type Al<A, B, T> = (a: A, b: B) => T
    `
    processor.process(makeSourceFile(contents))
    const expected = [alias_('Al', false, fun_(refs_('A', 'B'), ref_('T')), [gen_('A'), gen_('B'), gen_('T')])]
    expect(generator.aliases).toMatchObject(expected)
  })
  it('should throw if generics are constrained in type aliases', () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = `
    type AI<A extends I> = C<AI>
    `
    expect(() => processor.process(makeSourceFile(contents))).toThrowError(
      'Generic constraints are not supported with aliases',
    )
  })
  it('should support arrays in aliases', () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = `
    export type NumberArray1 = Int64[]
    export type NumberArray2 = readonly Int64[]
    `
    processor.process(makeSourceFile(contents))
    const expected = [
      alias_('NumberArray1', true, arr_(int64_(), false)),
      alias_('NumberArray2', true, arr_(int64_(), true)),
    ]
    expect(generator.aliases).toMatchObject(expected)
  })
  it('should throw if function alias with unspecified argument type is met', () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = `
    type F = (a) => void
    `
    expect(() => {
      processor.process(makeSourceFile(contents))
    }).toThrowError(/Functions with parameters with non-specified types are not supported/)
  })

  // FUNCTIONS
  it('should extract functions', () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = `
    export function f1(): void {}
    function f2(a: string, b: B): void {}
    export function f3(a: string, b: (n: Double) => string): (b: boolean, n: Int32) => void {}
    function f4(a: string): Int64;

    function f5(a: string = 'a'): void {}
    function f6(a: boolean = true): void {}
    function f7(a: boolean = false): void {}
    function f8(a: Double = 10.5): void {}
    function f9(a: Int32 = 10): void {}
    function f10(a: Int64 = 10): void {}

    function f11(a: Nullable<string> = null): void {}
    function f12(a: Nullable<boolean> = null): void {}
    function f13(a: Nullable<Double> = null): void {}
    function f14(a: Nullable<Int32> = null): void {}
    function f15(a: Nullable<Int64> = null): void {}
    `
    processor.process(makeSourceFile(contents))
    const functionArg = funArg('b', [double_()], str_())
    const functionRet = fun_([bool_(), int32_()], void_())
    const expected = [
      function_('f1', [], void_(), [], true, block_([])),
      function_('f2', [strArg('a'), refArg('b', 'B')], void_(), [], false, block_()),
      function_('f3', [strArg('a'), functionArg], functionRet, [], true, block_()),
      function_('f4', [strArg('a')], int64_(), [], false),

      function_('f5', [strArg('a', 'a')], void_(), [], false, block_()),
      function_('f6', [boolArg('a', true)], void_(), [], false, block_()),
      function_('f7', [boolArg('a', false)], void_(), [], false, block_()),
      function_('f8', [doubleArg('a', 10.5)], void_(), [], false, block_()),
      function_('f9', [int32Arg('a', 10)], void_(), [], false, block_()),
      function_('f10', [int64Arg('a', 10)], void_(), [], false, block_()),

      function_('f11', [nullArg(strArg('a'), null)], void_(), [], false, block_()),
      function_('f12', [nullArg(boolArg('a'), null)], void_(), [], false, block_()),
      function_('f13', [nullArg(doubleArg('a'), null)], void_(), [], false, block_()),
      function_('f14', [nullArg(int32Arg('a'), null)], void_(), [], false, block_()),
      function_('f15', [nullArg(int64Arg('a'), null)], void_(), [], false, block_()),
    ]
    expect(generator.functions).toMatchObject(expected)
  })
  it('should support generics in functions', () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = `
    function f<T, B, R>(a: T, b: B): R {}
    `
    processor.process(makeSourceFile(contents))
    const expected = [
      function_(
        'f',
        [refArg('a', 'T'), refArg('b', 'B')],
        ref_('R'),
        [gen_('T'), gen_('B'), gen_('R')],
        false,
        block_(),
      ),
    ]
    expect(generator.functions).toMatchObject(expected)
  })
  it('should support arrays in functions', () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents =
      // tslint:disable-next-line: max-line-length
      'export function f<T>(t: T[], n: Array<Int32>, r: readonly Int64[], k: ReadonlyArray<string>): ReadonlyArray<string[]> {}'
    processor.process(makeSourceFile(contents))
    const expected = [
      function_(
        'f',
        [
          arrArg('t', ref_('T'), false),
          arrArg('n', int32_(), false),
          arrArg('r', int64_(), true),
          arrArg('k', str_(), true),
        ],
        arr_(arr_(str_(), false), true),
        [gen_('T')],
        true,
        block_(),
      ),
    ]
    expect(generator.functions).toMatchObject(expected)
  })
  it("should throw if there're untyped arguments in functions", () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = `
    function f(t, n: number): string { return '' }
    `
    expect(() => {
      processor.process(makeSourceFile(contents))
    }).toThrowError(/Untyped function parameters are not supported/)
  })
  it("should throw if the function doesn't have return type specified", () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = `
    function f(t: string, n: number) { return '' }
    `
    expect(() => {
      processor.process(makeSourceFile(contents))
    }).toThrowError(/Functions with unspecified return types are not supported/)
  })
  it('should generate a function with array default value', () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = `
    function f(a: number[] = [1,2,3]): void { }
    `
    processor.process(makeSourceFile(contents))
    const expected = [
      function_('f', [arrArg('a', int32_(), false, lit_([lit_(1), lit_(2), lit_(3)]))], void_(), [], false, block_()),
    ]
    expect(generator.functions).toStrictEqual(expected)
  })
  it('should generate a function with reference default value', () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = `
    function f(a: A = new A()): void { }
    `
    processor.process(makeSourceFile(contents))
    const expected = [
      function_(
        'f',
        [
          refArg('a', 'A', {
            kind: TSExpressionKind.ObjectCreation,
            object: {
              kind: TSExpressionKind.Identifier,
              name: 'A',
            } as TSIdentifier,
            args: [],
            typeArguments: [],
          } as TSObjectCreationExpression),
        ],
        void_(),
        [],
        false,
        block_(),
      ),
    ]
    expect(generator.functions).toStrictEqual(expected)
  })
  it('should generate a function with enum default value', () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = `
    enum Options { A, B }
    function f(a: Options = Options.A): void { }
    `
    processor.process(makeSourceFile(contents))
    const expected = [
      function_(
        'f',
        [
          refArg('a', 'Options', {
            kind: TSExpressionKind.MemberAccess,
            object: {
              kind: TSExpressionKind.Identifier,
              name: 'Options',
            },
            member: {
              kind: TSExpressionKind.Identifier,
              name: 'A',
            },
            nullableTypeHint: undefined,
            objectTypeHint: undefined,
            optionalChaining: false,
          } as TSMemberAccessExpression),
        ],
        void_(),
        [],
        false,
        block_(),
      ),
    ]
    expect(generator.functions).toStrictEqual(expected)
  })
  // NULLABLE
  it('should support nullable types', () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = `
    export function f1(a: Nullable<string>): Nullable<Int32> {}
    function f2(a: string): Nullable<Nullable<string>> {}
    export function f3(): Nullable<(b: boolean, n: Int64) => void> {}
    `
    processor.process(makeSourceFile(contents))
    const expected = [
      function_('f1', [nullArg(strArg('a'))], null_(int32_()), [], true, block_()),
      function_('f2', [strArg('a')], null_(null_(str_())), [], false, block_()),
      function_('f3', [], null_(fun_([bool_(), int64_()], void_())), [], true, block_()),
    ]
    expect(generator.functions).toMatchObject(expected)
  })

  // UNION
  it('should support nullable types through undefined', () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = `
    export function f1(a: string | undefined): Int32 | undefined {}
    export function f3(): ((b: boolean, n: Int64) => void) | null {}
    `
    processor.process(makeSourceFile(contents))
    const expected = [
      function_('f1', [nullArg(strArg('a'))], null_(int32_()), [], true, block_()),
      function_('f3', [], null_(fun_([bool_(), int64_()], void_())), [], true, block_()),
    ]
    expect(generator.functions).toMatchObject(expected)
  })
  it('should throw if union type is other than with undefined or null', () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = 'export function f1(a: string | number): Int32 | boolean {}'
    expect(() => processor.process(makeSourceFile(contents))).toThrowError(
      "Unsupported union type. The ones that are supported is with 'undefined' or 'null'.",
    )
  })

  // ENUMS
  it('should support enums', () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = `
    export enum E {
      A,
      B,
      C,
    }
    `
    processor.process(makeSourceFile(contents))
    expect(generator.enums).toMatchObject([enum_('E', true, 'A', 'B', 'C')])
  })
  it('should support memberless enums', () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = `
    enum E { }
    `
    processor.process(makeSourceFile(contents))
    expect(generator.enums).toMatchObject([enum_('E', false)])
  })
  it('should support enum with number raw type', () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = `
    export enum E {
      A = 10,
      B = 20,
    }
    `
    const expected = [
      enumWithValues_('E', true, [
        { name: 'A', value: 10 },
        { name: 'B', value: 20 },
      ]),
    ]
    processor.process(makeSourceFile(contents))
    expect(generator.enums).toMatchObject(expected)
  })
  it('should support enum with string raw type', () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = `
    enum E {
      A = 'AA',
      B = 'BB',
    }
    `
    const expected = [
      enumWithValues_('E', false, [
        { name: 'A', value: 'AA' },
        { name: 'B', value: 'BB' },
      ]),
    ]
    processor.process(makeSourceFile(contents))
    expect(generator.enums).toMatchObject(expected)
  })
  it('should throw if enum with raw types but not string or number', () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = `
    enum B {
      X,
      Z
    }
    enum E {
      A = B.X,
      B = B.Z,
    }
    `
    expect(() => {
      processor.process(makeSourceFile(contents))
    }).toThrowError(/Only strings and numbers are supported as enum raw types/)
  })
  it('should throw if only some but not all members are valued', () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = `
    enum E {
      A,
      B = 10,
      C = 20,
    }
    `
    expect(() => {
      processor.process(makeSourceFile(contents))
    }).toThrowError(/Expected to have enum members of type <None>\. Found others: B, C/)
  })
  it('should throw if members are valued with different types (String first)', () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = `
    enum E {
      A = 'hello',
      B = 10,
      C = 20,
    }
    `
    expect(() => {
      processor.process(makeSourceFile(contents))
    }).toThrowError(/Expected to have enum members of type String\. Found others: B, C/)
  })
  it('should throw if members are valued with different types (Number first)', () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = `
    enum E {
      A = 10,
      B = 'hello',
      C = 20,
    }
    `
    expect(() => {
      processor.process(makeSourceFile(contents))
    }).toThrowError(/Expected to have enum members of type Number\. Found others: B/)
  })

  // TUPLES
  it('should throw if tuple type is met', () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = `
    interface A {
      m(a: string, b: number): [string, int]
    }
    `
    expect(() => {
      processor.process(makeSourceFile(contents))
    }).toThrowError(/Tuples are not supported/)
  })

  // UNSUPPORTED TYPE
  it('should throw if unsupported type is met', () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = `
    type A = {[key: string]: string}
    `
    expect(() => {
      processor.process(makeSourceFile(contents))
    }).toThrowError(/Unsupported type/)
  })

  // GENERICS
  it('should support reference types parametrization with generics', () => {
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '')
    const contents = `
    export type NumberArray = readonly Int32[]
    export type ParameterizedMyClass = MyClass<MyType, string, Map<string, I2>>
    `
    processor.process(makeSourceFile(contents))
    const expected = [
      alias_('NumberArray', true, arr_(int32_(), true)),
      alias_(
        'ParameterizedMyClass',
        true,
        ref_('MyClass', [ref_('MyType'), str_(), ref_('Map', [str_(), ref_('I2')])]),
      ),
    ]
    expect(generator.aliases).toMatchObject(expected)
  })
})
