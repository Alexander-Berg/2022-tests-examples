const hasValidHeaderOrigin = require('./hasValidHeaderOrigin');

const TESTS = [
    { headers: {}, result: true },
    { headers: { origin: 'https://frontend2.aandrosov-01-sas.dev.avto.ru' }, result: true },
    { headers: { origin: 'https://test.avto.ru' }, result: true },
    { headers: { origin: 'https://test2.avto.ru' }, result: true },
    { headers: { origin: 'https://www1.test.avto.ru' }, result: true },
    { headers: { origin: 'https://m.test2.avto.ru' }, result: true },
    { headers: { origin: 'https://auto.ru' }, result: true },
    { headers: { origin: 'https://moscow.auto.ru' }, result: true },
    { headers: { origin: 'yandex.ru' }, result: true },
    { headers: { origin: 'https://service.vertis.yandex-team.ru' }, result: true },

    { headers: { origin: 'http://auto.ru.com' }, result: false },
    { headers: { origin: 'https://auto.ru.com' }, result: false },
    { headers: { origin: 'http://1auto.ru' }, result: false },
    { headers: { origin: 'http://aauto.ru' }, result: false },
    { headers: { origin: 'http://igetsend.ru' }, result: false },
    { headers: { origin: 'https://st.yandexadexchange.net' }, result: false },
    { headers: { origin: 'http://x24.ijquery11.com' }, result: false },

];

TESTS.forEach((testCase) => {
    it(`should return ${ testCase.result } for ${ JSON.stringify(testCase.headers) }`, () => {
        expect(hasValidHeaderOrigin(testCase.headers)).toEqual(testCase.result);
    });
});
