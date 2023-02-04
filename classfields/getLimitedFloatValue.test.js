const getLimitedFloatValue = require('./getLimitedFloatValue');

const TEST_CASES = [
    {
        title: 'должен вернуть пустую строку, если передано не число',
        value: 'not_num',
        limit: 40,
        precision: undefined,
        expected: '',
    },

    {
        title: 'должен вернуть пустую строку, если передано undefined',
        value: undefined,
        limit: 40,
        precision: undefined,
        expected: '',
    },

    {
        title: 'должен вернуть лимит, если передано число больше лимита',
        value: 100,
        limit: 40,
        precision: undefined,
        expected: 40,
    },

    {
        title: 'должен вернуть число с точкой на конце, если оно меньше лимита',
        value: '30.',
        limit: 40,
        precision: undefined,
        expected: '30.',
    },

    {
        title: 'должен вернуть лимит, если число с точкой больше лимита',
        value: '41.',
        limit: 40,
        precision: undefined,
        expected: 40,
    },

    {
        title: 'должен оставить 3 знака после запятой',
        value: 30.0085,
        limit: 40,
        precision: 3,
        expected: '30.008',
    },
];

TEST_CASES.forEach(({ expected, limit, precision, title, value }) => {
    it(`getLimitedFloatValue ${ title } `, () => {
        expect(getLimitedFloatValue(value, limit, precision)).toEqual(expected);
    });
});

it(`getLimitedFloatValue должен выбросить исключение, если limit не задан`, () => {
    expect(() => getLimitedFloatValue(50)).toThrow('Не передано значение limit');
});
