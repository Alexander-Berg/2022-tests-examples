import linkMag from 'auto-core/router/mag.auto.ru/react/link';

import getRetpath from './getRetpath';

const INDEX_LINK = linkMag('mag-index');

const SUCCES_TEST_CASES = [
    { retpath: 'https://auto.ru/cars/all', description: 'должен вернуть ссылку на листинг' },
    { retpath: 'https://test.avto.ru/history', description: 'долежн вернуть ссылку на history' },
    { retpath: 'https://m.keka.peka.dev.avto.ru/garage', description: 'долежн вернуть ссылку на garage' },
];

const FAILED_TEST_CASES = [
    'pornhub.com',
    'https://pornhub.com',
    'mag.keka.peka.dev.avto.ru/garage',
    'http://m.keka.peka.dev.avto.ru/garage',
    'kekapeka123',
];

SUCCES_TEST_CASES.forEach((testCase) => {
    it(testCase.description, () => {
        const result = getRetpath({ retpath: testCase.retpath });

        expect(result).toBe(testCase.retpath);
    });
});

it('Должен вернуть ссылку на главную страницу журнала', () => {
    FAILED_TEST_CASES.forEach((testCase) => {
        const result = getRetpath({ retpath: testCase });

        expect(result).toBe(INDEX_LINK);
    });
});
