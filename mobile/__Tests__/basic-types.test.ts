import {
  areLocalTypesEqual,
  materializeLocalTypeWithGenerics,
  updateGenericTypeMappings,
} from '../src/generators-model/basic-types'

import {
  any_,
  arr_,
  bool_,
  double_,
  fun_,
  gen_,
  int32_,
  int64_,
  null_,
  ref_,
  str_,
  throwing_,
  void_,
} from './__helpers__/test-helpers'

describe(areLocalTypesEqual, () => {
  it('should check if kinds are equal', () => {
    expect(areLocalTypesEqual(int32_(), int32_())).toBe(true)
    expect(areLocalTypesEqual(int32_(), ref_('A'))).toBe(false)
  })
  it('should check if primitive types are equal', () => {
    const allPrimities = [int32_(), int64_(), bool_(), double_(), str_(), void_(), any_()]
    for (const lhs of allPrimities) {
      for (const rhs of allPrimities) {
        expect(areLocalTypesEqual(lhs, rhs)).toBe(lhs.name === rhs.name)
      }
    }
  })
  it('should check if reference types are equal', () => {
    expect(
      areLocalTypesEqual(
        ref_('A', [ref_('B', [str_(), int32_()]), ref_('C')]),
        ref_('A', [ref_('B', [str_(), int32_()]), ref_('C')]),
      ),
    ).toBe(true)
    expect(
      areLocalTypesEqual(
        ref_('A', [ref_('B', [str_(), int32_()]), ref_('C')]),
        ref_('A', [ref_('B', [str_(), bool_()]), ref_('C')]),
      ),
    ).toBe(false)
    expect(
      areLocalTypesEqual(
        ref_('A', [ref_('B', [str_()]), ref_('C')]),
        ref_('A', [ref_('B', [str_(), bool_()]), ref_('C')]),
      ),
    ).toBe(false)
    expect(
      areLocalTypesEqual(
        ref_('A', [ref_('D', [str_()]), ref_('C')]),
        ref_('A', [ref_('B', [str_(), bool_()]), ref_('C')]),
      ),
    ).toBe(false)
    expect(areLocalTypesEqual(ref_('A', [ref_('D', [str_()]), ref_('C')]), ref_('A'))).toBe(false)
  })
  it('should check if nullable types are equal', () => {
    expect(
      areLocalTypesEqual(
        null_(ref_('A', [ref_('B', [str_(), int32_()]), ref_('C')])),
        null_(ref_('A', [ref_('B', [str_(), int32_()]), ref_('C')])),
      ),
    ).toBe(true)
    expect(areLocalTypesEqual(null_(ref_('A')), null_(ref_('B')))).toBe(false)
  })
  it('should check if throwing types are equal', () => {
    expect(
      areLocalTypesEqual(
        throwing_(ref_('A', [ref_('B', [str_(), int32_()]), ref_('C')])),
        throwing_(ref_('A', [ref_('B', [str_(), int32_()]), ref_('C')])),
      ),
    ).toBe(true)
    expect(areLocalTypesEqual(null_(ref_('A')), null_(ref_('B')))).toBe(false)
  })
  it('should check if array types are equal', () => {
    expect(areLocalTypesEqual(arr_(ref_('A'), true), arr_(ref_('A'), true))).toBe(true)
    expect(areLocalTypesEqual(arr_(ref_('A'), false), arr_(ref_('A'), false))).toBe(true)
    expect(areLocalTypesEqual(arr_(ref_('A'), true), arr_(ref_('A'), false))).toBe(false)
    expect(areLocalTypesEqual(arr_(ref_('A'), false), arr_(ref_('A'), true))).toBe(false)
    expect(areLocalTypesEqual(arr_(ref_('A'), true), arr_(ref_('B'), true))).toBe(false)
  })
  it('should check if function types are equal', () => {
    expect(areLocalTypesEqual(fun_([str_()], bool_()), fun_([str_()], bool_()))).toBe(true)
    expect(areLocalTypesEqual(fun_([], bool_()), fun_([str_()], bool_()))).toBe(false)
    expect(areLocalTypesEqual(fun_([str_()], bool_()), fun_([], bool_()))).toBe(false)
    expect(areLocalTypesEqual(fun_([str_()], bool_()), fun_([int32_()], bool_()))).toBe(false)
    expect(areLocalTypesEqual(fun_([str_()], bool_()), fun_([str_()], int32_()))).toBe(false)
  })
})

describe(materializeLocalTypeWithGenerics, () => {
  it('should not materialize primitive types', () => {
    const allPrimities = [int32_(), int64_(), bool_(), double_(), str_(), void_(), any_()]
    for (const primitive of allPrimities) {
      expect(materializeLocalTypeWithGenerics(primitive, [[gen_(primitive.name), ref_('A')]])).toBe(primitive)
    }
  })
  it('should materialize reference types', () => {
    expect(materializeLocalTypeWithGenerics(ref_('A'), [[gen_('B'), ref_('$A')]])).toMatchObject(ref_('A'))
    expect(materializeLocalTypeWithGenerics(ref_('A'), [[gen_('A'), ref_('$A')]])).toMatchObject(ref_('$A'))
    expect(materializeLocalTypeWithGenerics(ref_('A', [ref_('B')]), [[gen_('B'), ref_('$B')]])).toMatchObject(
      ref_('A', [ref_('$B')]),
    )
    expect(
      materializeLocalTypeWithGenerics(ref_('A', [ref_('B1')]), [
        [gen_('B1'), ref_('$B1')],
        [gen_('B2'), ref_('$B2')],
      ]),
    ).toMatchObject(ref_('A', [ref_('$B1')]))
  })
  it('should materialize nullable types', () => {
    expect(materializeLocalTypeWithGenerics(null_(ref_('A')), [[gen_('A'), ref_('$A')]])).toMatchObject(
      null_(ref_('$A')),
    )
  })
  it('should materialize throwing types', () => {
    expect(materializeLocalTypeWithGenerics(throwing_(ref_('A')), [[gen_('A'), ref_('$A')]])).toMatchObject(
      throwing_(ref_('$A')),
    )
  })
  it('should materialize array types', () => {
    expect(materializeLocalTypeWithGenerics(arr_(ref_('A'), false), [[gen_('A'), ref_('$A')]])).toMatchObject(
      arr_(ref_('$A'), false),
    )
  })
  it('should materialize function types', () => {
    expect(materializeLocalTypeWithGenerics(fun_([], ref_('A')), [[gen_('A'), ref_('$A')]])).toMatchObject(
      fun_([], ref_('$A')),
    )
    expect(materializeLocalTypeWithGenerics(fun_([ref_('A')], void_()), [[gen_('A'), ref_('$A')]])).toMatchObject(
      fun_([ref_('$A')], void_()),
    )
  })
})

describe(updateGenericTypeMappings, () => {
  it('should update generic type mappings', () => {
    expect(updateGenericTypeMappings([[gen_('T'), ref_('U')]], [[gen_('A'), ref_('T')]])).toMatchObject([
      [gen_('A'), ref_('U')],
    ])
    expect(updateGenericTypeMappings([[gen_('$T'), ref_('U')]], [[gen_('A'), ref_('T')]])).toMatchObject([
      [gen_('A'), ref_('T')],
    ])
  })
})
