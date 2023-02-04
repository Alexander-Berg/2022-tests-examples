const phoneValidator = require('auto-core/fields/phone/phone-validator');

const TESTS = [
    { test: '89264247766', result: true },
    { test: '8926424776', result: false },
    { test: '+79264247766', result: true },
    { test: '+7926424776', result: false },
    { test: '79264247766', result: true },
    { test: '8 (926) 424 77 66', result: true },
    { test: '8 (926) 424-77-66', result: true },
    { test: '(926) 424 77 66', result: true },
    { test: '(926) 424 77 6', result: false },
    { test: '+7 926 424 77 66', result: true },
    { test: '7 926 424-77-66', result: true },
    { test: '+79060352130:1', result: true }, // добавочный номер
    { test: '+79060352130 :1', result: true }, // добавочный номер
    { test: '+375 17 222-4980', result: true }, // белорусский номер
    { test: '+375 17 222-4980 : 23', result: true }, // белорусский номер + добавочный
];

TESTS.forEach((testCase) => {
    it(`should refine "${ testCase.test }" as "${ testCase.result }"`, () => {
        expect(phoneValidator(testCase.test)).toEqual(testCase.result);
    });
});
