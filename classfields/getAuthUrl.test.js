const authUrl = require('./getAuthUrl');

const TESTS = [
    {
        options: {
            authPath: '/login/',
            currentHost: 'test.avto.ru',
            currentUrl: '/url/',
        },
        result: 'https://autoru_frontend.auth_domain/login/?r=https%3A%2F%2Ftest.avto.ru%2Furl%2F',
    },
    // дальше какие-то экзотические кейсы, непонятно применимы ли они к реальности
    {
        options: {
            authPath: '/login',
            currentHost: 'test.avto.ru',
            currentUrl: '/url/',
        },
        result: 'https://autoru_frontend.auth_domain/login/?r=https%3A%2F%2Ftest.avto.ru%2Furl%2F',
    },
    {
        options: {
            authPath: 'login/',
            currentHost: 'test.avto.ru',
            currentUrl: '/url/',
        },
        result: 'https://autoru_frontend.auth_domain/login/?r=https%3A%2F%2Ftest.avto.ru%2Furl%2F',
    },
    {
        options: {
            authPath: 'login/',
            authQuery: { r: '/my/' },
            currentHost: 'test.avto.ru',
            currentUrl: '/url/',
        },
        result: 'https://autoru_frontend.auth_domain/login/?r=https%3A%2F%2Ftest.avto.ru%2Fmy%2F',
    },
];

TESTS.forEach((testCase) => {
    it(`должен сгенерировать урл ${ testCase.result } для ${ JSON.stringify(testCase.options) }`, () => {
        expect(authUrl((testCase.options))).toEqual(testCase.result);
    });
});
