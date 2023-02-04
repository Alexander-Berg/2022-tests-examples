import {first, isFunction} from "../src/utils";

test("first", () => {
    expect(first()).toEqual(undefined);
    expect(first([])).toEqual(undefined);
    expect(first({})).toEqual(undefined);
    expect(first([42, 43])).toEqual(42);
});

test("isFunction", () => {
    expect(isFunction()).toEqual(false);
    expect(
        isFunction(() => {
            /* empty */
        })
    ).toEqual(true);
});
