const phoneRefiner = require('auto-core/fields/phone/phone-refiner');

const TESTS = [
    { test: '89264247766', result: '89264247766' },
    { test: '+79264247766', result: '79264247766' },
    { test: '79264247766', result: '79264247766' },
    { test: '8 (926) 424 77 66', result: '89264247766' },
    { test: '+7 926 424 77 66', result: '79264247766' },
    { test: '7 926 424-77-66', result: '79264247766' },
    { test: '+79060352130:1', result: '79060352130' }, // добавочный номер
    { test: '8987+79878765434', result: '79878765434' }, // отрезаем лишнее до +7
];

TESTS.forEach((testCase) => {
    it(`should refine "${ testCase.test }" as "${ testCase.result }"`, () => {
        expect(phoneRefiner(testCase.test)).toEqual(testCase.result);
    });
});
