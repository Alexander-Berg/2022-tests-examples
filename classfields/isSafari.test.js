/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

/* eslint-disable max-len */
const isSafari = require('./isSafari');

let originalWindowUserAgent;

beforeEach(() => {
    originalWindowUserAgent = global.navigator.userAgent;
});

afterEach(() => {
    global.navigator.userAgent = originalWindowUserAgent;
});

const testCases = [
    { title: 'chrome for android', expectedResult: false, userAgent: 'Mozilla/5.0 (Linux; Android 4.0.4; Galaxy Nexus Build/IMM76B) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.133 Mobile Safari/535.19' },
    { title: 'chrome desktop', expectedResult: false, userAgent: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.108 Safari/537.36' },
    // все браузеры на айос это всего лишь обертка над сафари, разницы в них нет
    { title: 'chrome for ios', expectedResult: true, userAgent: 'Mozilla/5.0 (iPhone; CPU iPhone OS 10_3 like Mac OS X) AppleWebKit/603.1.23 (KHTML, like Gecko) Version/10.0 Mobile/14E5239e Safari/602.1' },
    { title: 'safari desktop', expectedResult: true, userAgent: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_4) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/12.1 Safari/605.1.15' },
    { title: 'safari mobile', expectedResult: true, userAgent: 'Mozilla/5.0 (iPhone; CPU iPhone OS 12_1_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/12.0 Mobile/15E148 Safari/604.1' },
    { title: 'yabrowser_mobile', expectedResult: false, userAgent: 'Mozilla/5.0 (iPhone; CPU iPhone OS 13_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.0 YaBrowser/20.7.1.222.10 Mobile/15E148 Safari/604.1' },
];

testCases.forEach(({ expectedResult, title, userAgent }) => {
    it(`правильно работает для "${ title }"`, () => {
        global.navigator.__defineGetter__('userAgent', function() {
            return userAgent;
        });
        expect(isSafari()).toBe(expectedResult);
    });
});
