import transformNaviConfig from "../helpers/transformNaviConfig"

function transform(object: any): any {
    transformNaviConfig(object)
    return object
}

it("transform", () => {
    expect(transform({})).toStrictEqual({})
    expect(transform([])).toStrictEqual([])
    expect(transform(0)).toStrictEqual(0)

    expect(transform({a: {}})).toStrictEqual({a: {}})
    expect(transform({a: []})).toStrictEqual({a: []})
    expect(transform({a: ""})).toStrictEqual({a: ""})
    expect(transform({a: 0})).toStrictEqual({a: 0})
    expect(transform({a: false})).toStrictEqual({a: false})
    expect(transform({a: null})).toStrictEqual({a: null})

    expect(transform({a__optional__: {}})).toStrictEqual({})
    expect(transform({a__optional__: []})).toStrictEqual({})
    expect(transform({a__optional__: ""})).toStrictEqual({})
    expect(transform({a__optional__: null})).toStrictEqual({})

    expect(transform({a__optional__: 0})).toStrictEqual({a: 0})
    expect(transform({a__optional__: false})).toStrictEqual({a: false})

    expect(transform({a: {b__optional__: ""}})).toStrictEqual({a: {}})
    expect(transform({a: {b__optional__: {c: "d"}}})).toStrictEqual({a: {b: {c: "d"}}})

    expect(transform({a: {b__optional__: {c__optional__: ""}}})).toStrictEqual({a: {}})
    expect(transform({a: {b__optional__: {c: ""}}})).toStrictEqual({a: {}})

    expect(transform({a: {b__optional__: [{c: ""}, {d: [], e: {}}]}})).toStrictEqual({a: {}})

    expect(() => transform({a: "", a__optional__: ""})).toThrow('config has both "a" and "a__optional__"')
})
