const phoneFormatter = require('auto-core/fields/phone/phone-formatter-e164');

const TESTS = [
    { test: '', result: '' },
    { test: undefined, result: '' },
    { test: '89264247766', result: '+79264247766' }, // заменяет 8 в начале на +7
    { test: '9264247766', result: '+79264247766' }, // добавляет +7 для 10-ти значного номера
    { test: '+79264247766', result: '+79264247766' },
    { test: '8987+79878765434', result: '+79878765434' }, // отрезаем лишнее до +7
    { test: '79264247766', result: '+79264247766' }, // заменяет 7 в начале на +7
    { test: '84832123456', result: '+74832123456' },
    { test: '8 (926) 424 77 66', result: '+79264247766' },
    { test: '+7 926 424 77 66', result: '+79264247766' },
    { test: '7 926 424-77-66', result: '+79264247766' },
    { test: '+79060352130:1', result: '+79060352130' }, // добавочный номер
    { test: '123456', result: '+123456' }, // unknown number
    { test: '87654321', result: '+87654321' }, // unknown number
];

TESTS.forEach((testCase) => {
    it(`should format "${ testCase.test }" as "${ testCase.result }"`, () => {
        expect(phoneFormatter(testCase.test)).toEqual(testCase.result);
    });
});
