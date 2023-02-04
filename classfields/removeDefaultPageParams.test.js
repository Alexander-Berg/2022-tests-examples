const removeDefaultPageParams = require('./removeDefaultPageParams');
const LISTING_OUTPUT_TYPE = require('auto-core/data/listing/OutputTypes').default;

const TEST_CASES = [
    {
        name: 'удаляем дефолтные параметры для новых тачек',
        params: { page: 1, output_type: LISTING_OUTPUT_TYPE.MODELS, section: 'new', category: 'cars' },
        expectedResult: { section: 'new', category: 'cars' },
    },
    {
        name: 'удаляем дефолтные параметры для других категорий и секций',
        params: { page: 1, output_type: LISTING_OUTPUT_TYPE.LIST, section: 'all', moto_category: 'scooters' },
        expectedResult: { section: 'all', moto_category: 'scooters' },
    },
    {
        name: 'оставляем параметры для новых тачек если они не дефолтные',
        params: { page: 2, output_type: LISTING_OUTPUT_TYPE.LIST, section: 'new', category: 'cars' },
        expectedResult: { page: 2, output_type: LISTING_OUTPUT_TYPE.LIST, section: 'new', category: 'cars' },
    },
    {
        name: 'оставляем параметры для других секций и категорий если они не дефолтные',
        params: { page: 2, output_type: LISTING_OUTPUT_TYPE.TABLE, section: 'all', moto_category: 'scooters' },
        expectedResult: { page: 2, output_type: LISTING_OUTPUT_TYPE.TABLE, section: 'all', moto_category: 'scooters' },
    },
];

TEST_CASES.forEach(test => {
    it(`${ test.name }`, () => {
        expect(removeDefaultPageParams(test.params)).toEqual(test.expectedResult);
    });
});

const TEST_CASES_EXP = [
    {
        name: 'удаляем дефолтные параметры для новых тачек',
        params: { page: 1, output_type: LISTING_OUTPUT_TYPE.MODELS, section: 'new', category: 'cars' },
        expectedResult: { section: 'new', category: 'cars' },
    },
    {
        name: 'удаляем дефолтные параметры для других категорий и секций',
        params: { page: 1, output_type: LISTING_OUTPUT_TYPE.CAROUSEL, section: 'all', moto_category: 'scooters' },
        expectedResult: { section: 'all', moto_category: 'scooters' },
    },
    {
        name: 'оставляем параметры для новых тачек если они не дефолтные',
        params: { page: 2, output_type: LISTING_OUTPUT_TYPE.CAROUSEL, section: 'new', category: 'cars' },
        expectedResult: { page: 2, output_type: LISTING_OUTPUT_TYPE.CAROUSEL, section: 'new', category: 'cars' },
    },
    {
        name: 'оставляем параметры для других секций и категорий если они не дефолтные',
        params: { page: 2, output_type: LISTING_OUTPUT_TYPE.TABLE, section: 'all', moto_category: 'scooters' },
        expectedResult: { page: 2, output_type: LISTING_OUTPUT_TYPE.TABLE, section: 'all', moto_category: 'scooters' },
    },
];

TEST_CASES_EXP.forEach(test => {
    it(`${ test.name }`, () => {
        expect(removeDefaultPageParams(test.params, true)).toEqual(test.expectedResult);
    });
});
