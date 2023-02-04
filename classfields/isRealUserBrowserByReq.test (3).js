'use strict';

const isRealUserBrowserByReq = require('./isRealUserBrowserByReq');

const TESTS = [
    { req: require('../../../mocks/uatraits/chrome_41_gpsi_android'), result: false },
    { req: require('../../../mocks/uatraits/chrome_41_gpsi_linux'), result: false },
    { req: require('../../../mocks/uatraits/chrome_69_lighthouse_android'), result: false },
    { req: require('../../../mocks/uatraits/chrome_69_lighthouse_linux'), result: false },
    { req: require('../../../mocks/uatraits/chromium_71'), result: true },
    { req: require('../../../mocks/uatraits/curl'), result: false },
    { req: require('../../../mocks/uatraits/feedvalidator'), result: false },
    { req: require('../../../mocks/uatraits/google_site_verification'), result: false },
    { req: require('../../../mocks/uatraits/google_structured_data_testing_tool'), result: false },
    { req: require('../../../mocks/uatraits/googlebot'), result: false },
    { req: require('../../../mocks/uatraits/telegrambot'), result: false },
    { req: require('../../../mocks/uatraits/whatsapp_2.18.111_i'), result: false },
];

TESTS.forEach((testCase) => {
    it(`должен вернуть ${ testCase.result } для "${ testCase.req.headers['user-agent'] }"`, () => {
        expect(isRealUserBrowserByReq(testCase.req)).toEqual(testCase.result);
    });
});

it('должен ответить true, если ничего не определилось', () => {
    // Это тест на fallback
    expect(isRealUserBrowserByReq({})).toEqual(true);
});
