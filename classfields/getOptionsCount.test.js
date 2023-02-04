const getOptionsCount = require('./getOptionsCount');

const OPTIONS_COUNT = 39;

const COMPLECTATION = {
    option_count: OPTIONS_COUNT,
};

it('должен вернуть правильное количество опций', () => {
    expect(getOptionsCount(COMPLECTATION)).toBe(OPTIONS_COUNT);
});

it('должен вернуть undefined если опций нет', () => {
    expect(getOptionsCount({})).toBeUndefined();
});
