const isAutoruUrl = require('./isAutoruUrl');

const TESTS = [
    { url: undefined, result: false },
    { url: '', result: false },
    { url: 'https://autoru-frontend.stormtrooper.dev.avto.ru', result: true },
    { url: 'https://m.autoru-frontend.stormtrooper.dev.avto.ru', result: true },
    { url: 'https://autoru-frontend.local.dev.avto.ru', result: true },
    { url: 'https://autoru-frontend.local.dev.avto.ru/cars/used/?asddd', result: true },
    { url: 'https://test.avto.ru', result: true },
    { url: 'https://test2.avto.ru', result: true },
    { url: 'https://www1.test.avto.ru', result: true },
    { url: 'https://m.test2.avto.ru', result: true },
    { url: 'https://auto.ru', result: true },
    { url: 'https://mag.auto.ru', result: true },
    { url: 'https://moscow.auto.ru', result: true },

    { url: 'http://auto.ru.com', result: false },
    { url: 'https://auto.ru.com', result: false },
    { url: 'http://1auto.ru', result: false },
    { url: 'http://auto.ru', result: false },
    { url: 'http://aauto.ru', result: false },
    { url: 'http://igetsend.ru', result: false },
    { url: 'https://st.yandexadexchange.net', result: false },
    { url: 'http://x24.ijquery11.com', result: false },

];

TESTS.forEach((testCase) => {
    it(`должен вернуть ${ testCase.result } for ${ JSON.stringify(testCase.url) }`, () => {
        expect(isAutoruUrl(testCase.url)).toEqual(testCase.result);
    });
});
