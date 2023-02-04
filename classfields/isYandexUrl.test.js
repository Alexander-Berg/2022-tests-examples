const isYandexUrl = require('./isYandexUrl');

const locations = [ 'ru', 'com', 'com.tr', 'co.il' ];

const TESTS = [
    ...locations.map(loc => ({
        url: 'https://yandex.' + loc, result: true,
    })),
    ...locations.map(loc => ({
        url: 'https://project.yandex.' + loc, result: true,
    })),
    ...locations.map(loc => ({
        url: 'https://sub.project.yandex.' + loc, result: true,
    })),
    { url: 'http://yandex.ru', result: true },
    { url: 'yandex.ru', result: true },

    { url: '', result: false },
    { url: 'http://auto.ru', result: false },
    { url: 'http://aauto.ru', result: false },
    { url: 'http://igetsend.ru', result: false },
    { url: 'https://st.yandexadexchange.net', result: false },
    { url: 'http://x24.ijquery11.com', result: false },

];

TESTS.forEach((testCase) => {
    it(`должен вернуть ${ testCase.result } for ${ JSON.stringify(testCase.url) }`, () => {
        expect(isYandexUrl(testCase.url)).toEqual(testCase.result);
    });
});
