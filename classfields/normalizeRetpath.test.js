const normalizeRetpath = require('./normalizeRetpath');

const TESTS = [
    {
        args: [
            {
                headers: { 'x-forwarded-host': 'auto.ru' },
                query: { r: 'https://auto.ru/cars/all/' },
            },
        ],
        result: 'https://auto.ru/cars/all/',
    },
    {
        args: [
            {
                headers: { 'x-forwarded-host': 'autoru-frontend.local.dev.avto.ru' },
                query: { r: 'https://autoru-frontend.local.dev.avto.ru/cars/all/' },
            },
        ],
        result: 'https://autoru-frontend.local.dev.avto.ru/cars/all/',
    },
    {
        args: [
            {
                headers: { 'x-forwarded-host': 'auth.auth.aandrosov.dev.vertis.yandex.net' },
                // eslint-disable-next-line max-len
                query: { r: 'https://auth.auto.ru/login/?r=https%3A%2F%2Fm.auto.ru%2Fcars%2Fnew%2Fgroup%2Fbmw%2Fx5%2F21308274%2F21519497%2F1100617616-c29af678%2F%23open-chat%2F1100617616-c29af678' },
            },
        ],
        // eslint-disable-next-line max-len
        result: 'https://auth.auto.ru/login/?r=https%3A%2F%2Fm.auto.ru%2Fcars%2Fnew%2Fgroup%2Fbmw%2Fx5%2F21308274%2F21519497%2F1100617616-c29af678%2F%23open-chat%2F1100617616-c29af678',
    },
    {
        args: [
            {
                headers: { 'x-forwarded-host': 'auto.ru' },
                query: { r: '/login/' },
            },
        ],
        result: 'https://auto.ru/login/',
    },
    {
        args: [
            {
                headers: { 'x-forwarded-host': 'auto.ru' },
                query: { r: 'xaker.ru' },
            },
        ],
        result: 'https://auto.ru/xaker.ru',
    },
    // тест второго аргумента normalizeRetpath(req, location)
    {
        args: [
            {
                headers: { 'x-forwarded-host': 'auto.ru' },
                query: { r: 'xaker.ru' },
            },
            '/logout/',
        ],
        result: 'https://auto.ru/logout/',
    },
    {
        args: [
            {
                headers: { 'x-forwarded-host': 'autoru_frontend.base_domain' },
                query: { r: '////xaker.ru//' },
            },
        ],
        result: 'https://autoru_frontend.base_domain/',
    },
];

TESTS.forEach(testCase => {
    it(`должен вернуть ${ testCase.result } для ${ JSON.stringify(testCase.args) }`, () => {
        expect(normalizeRetpath(...testCase.args)).toEqual(testCase.result);
    });
});
