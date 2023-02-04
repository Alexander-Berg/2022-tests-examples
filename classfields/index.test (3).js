const phones = require('./');

const TEST_CASES_FOR_IS_PHONE = [
    { input: '+79264247766', result: true },
    { input: '79264247766', result: true },
    { input: '89264247766', result: true },
    { input: '9264247766', result: true },
    { input: '89264247766@ya.ru', result: false },
    { input: 'doochk@ya.ru', result: false },
];

describe('isPhone', () => {
    TEST_CASES_FOR_IS_PHONE.forEach(({ input, result }) => {
        it(`should return ${ result } for ${ input }`, () => {
            expect(phones.isPhone(input)).toEqual(result);
        });
    });
});

const TEST_CASES_FOR_FORMAT_PHONE = [

    // форматирование по мере ввода
    { input: '+7', result: '+7' },
    { input: '+79', result: '+7 9' },
    { input: '+799', result: '+7 99' },
    { input: '+7999', result: '+7 999' },
    { input: '+79999', result: '+7 999 9' },
    { input: '+799999', result: '+7 999 99' },
    { input: '+7999999', result: '+7 999 999' },
    { input: '+79999999', result: '+7 999 999-9' },
    { input: '+799999999', result: '+7 999 999-99' },
    { input: '+7999999999', result: '+7 999 999-99-9' },
    { input: '+79999999999', result: '+7 999 999-99-99' },

    // форматирование готового номера
    { input: '79999999999', result: '+7 999 999-99-99' },
    { input: '375999999999', result: '+375 99 999-99-99' },
];

describe('formatPhone', () => {
    TEST_CASES_FOR_FORMAT_PHONE.forEach(({ input, result }) => {
        it(`should return ${ result } for ${ input }`, () => {
            expect(phones.formatPhone(input)).toEqual(result);
        });
    });
});
