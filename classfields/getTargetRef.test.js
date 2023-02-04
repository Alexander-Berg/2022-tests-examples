const getTargetRef = require('./getTargetRef');

const TESTS = [
    { url: 'https://auto.ru/', result: 'https://auto.ru/' },
    { url: 'https://auto.ru/cars/used/', result: 'https://auto.ru/cars/used/' },
    { url: 'https://auto.ru/cars/used/?only-data=1', result: 'https://auto.ru/cars/used/' },
    { url: 'https://auto.ru/cars/used/?listingUrl=1', result: 'https://auto.ru/cars/used/' },
    { url: 'https://auto.ru/cars/used/?__blocks=1', result: 'https://auto.ru/cars/used/' },
    { url: 'https://auto.ru/cars/used/?crc=1', result: 'https://auto.ru/cars/used/' },
];

TESTS.forEach((testCase) => {
    it(`должен вернуть "${ testCase.result }" для "${ testCase.url }"`, () => {
        expect(getTargetRef(testCase.url)).toEqual(testCase.result);
    });
});
