const _ = require('lodash');

const check = require('./checkCarForYandexAuto');
const card = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');

const bunkerData = {
    geo: [
        1,
        10174,
    ],
    cars: [
        {
            name: 'ford_ecosport',
            year_from: 2011,
            url: 'https://example.com',
        },
    ],
};

describe('Проверка входных данных', () => {
    it('Вернет false без параметров', () => {
        const result = check();
        expect(result).toBeUndefined();
    });

    it('Вернет false c некорректными параметрами', () => {
        const result = check('test', '234');
        expect(result).toBeUndefined();
    });
});

describe('Проверка критериев', () => {
    it('Вернет false, если не совпадает марка', () => {
        const testCard = _.cloneDeep(card);
        testCard.vehicle_info.mark_info.code = 'audi';

        const result = check(testCard, bunkerData);
        expect(result).toBeUndefined();
    });

    it('Вернет false, если не совпадает модель', () => {
        const testCard = _.cloneDeep(card);
        testCard.vehicle_info.model_info.code = 'q1';

        const result = check(testCard, bunkerData);
        expect(result).toBeUndefined();
    });

    it('Вернет false, если машина новая', () => {
        const testCard = _.cloneDeep(card);
        testCard.state.mileage = 0;

        const result = check(testCard, bunkerData);
        expect(result).toBeUndefined();
    });

    it('Вернет false, если год выпуска раньше минимального', () => {
        const testCard = _.cloneDeep(card);
        testCard.documents.year = 2000;

        const result = check(testCard, bunkerData);
        expect(result).toBeUndefined();
    });

    it('Вернет false, если год выпуска позже максимального', () => {
        const testCard = _.cloneDeep(card);
        const testBunkerData = _.cloneDeep(bunkerData);
        testCard.documents.year = 2020;
        testBunkerData.cars[0]['year_to'] = 2018;

        const result = check(testCard, testBunkerData);
        expect(result).toBeUndefined();
    });

    it('Вернет false, если не совпадает регион', () => {
        const testCard = _.cloneDeep(card);
        testCard.seller.location.region_info.parent_ids = [ 4, 5 ];

        const result = check(testCard, bunkerData);
        expect(result).toBeUndefined();
    });
});

describe('Положительный результат', () => {
    it('Вернет объект из бункера, если машина подходит', () => {
        const result = check(card, bunkerData);
        expect(result).toEqual(bunkerData.cars[0]);
    });
});
