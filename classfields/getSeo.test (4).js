const getSeo = require('./getSeo');

const TESTS = [
    {
        pageType: 'my-reviews',
        result: {
            title: 'Отзывы',
            canonical: 'https://autoru_frontend.base_domain/my/reviews/',
        },
    },
    {
        pageType: 'sales',
        result: {
            title: 'Объявления',
            canonical: 'https://autoru_frontend.base_domain/my/all/',
        },
    },
    {
        pageType: 'my-wallet',
        result: {
            title: 'Кошелёк',
            canonical: 'https://autoru_frontend.base_domain/my/wallet/',
        },
    },
    {
        pageType: 'my-profile',
        result: {
            title: 'Настройки',
        },
    },
    {
        pageType: 'public-profile',
        result: {
            title: 'Страница пользователя',
        },
    },
    {
        pageType: 'my-credits',
        result: {
            title: 'Купить в кредит машину',
        },
    },
    {
        pageType: 'my-deals',
        result: {
            title: 'Мои сделки',
        },
    },
];

TESTS.forEach((testCase) => {
    it(`должен вернуть правильное сео для ${ testCase.pageType }`, () => {
        const state = {
            config: { data: { pageType: testCase.pageType } },
        };

        expect(getSeo(state)).toEqual(testCase.result);
    });
});
