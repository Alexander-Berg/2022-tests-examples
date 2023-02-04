const isYandexTeamUrl = require('./isYandexTeamUrl');

const TESTS = [
    { url: undefined, result: false },
    { url: '', result: false },
    { url: 'https://service.vertis.yandex-team.ru', result: true },
    { url: 'https://service.test.vertis.yandex-team.ru', result: true },
    { url: 'https://frontend2.aandrosov-01-sas.dev.vertis.yandex-team.ru', result: true },
    { url: 'https://frontend2.aandrosov-01-sas.dev.vertis.yandex-team.ru/some/', result: true },
    { url: 'https://service.frontend2.aandrosov-01-sas.dev.vertis.yandex-team.ru/some/', result: true },

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
        expect(isYandexTeamUrl(testCase.url)).toEqual(testCase.result);
    });
});
