const _ = require('lodash');

const prepareParamsForFooterGeoInfo = require('./prepareParamsForFooterGeoInfo');
const paramsMock = require('autoru-frontend/mockData/pageParams_cars.mock.js');

describe('prepareParamsForFooterGeoInfo', () => {
    it('уберет все лишние параметры', () => {
        expect(prepareParamsForFooterGeoInfo(paramsMock)).toEqual({
            category: 'cars',
            section: 'new',
        });
    });

    it('уберет category если есть moto_category или trucks_category', () => {
        const params = _.cloneDeep(paramsMock);
        params.trucks_category = 'lcv';

        expect(prepareParamsForFooterGeoInfo(params)).toEqual({
            trucks_category: 'lcv',
            section: 'new',
        });
    });

    it('уберет catalog_filter если там несколько тачек', () => {
        const params = _.cloneDeep(paramsMock);
        params.catalog_filter = [ { mark: 'audi' }, { mark: 'bmw' } ];

        expect(prepareParamsForFooterGeoInfo(params)).toEqual({
            category: 'cars',
            section: 'new',
        });
    });

    it('уберет из catalog_filter всё кроме марки/модели', () => {
        const params = _.cloneDeep(paramsMock);
        params.catalog_filter = [
            {
                mark: 'audi',
                model: 'a6',
                generation: '123',
                configuration: '456',
                tech_param: '789',
            },
        ];

        expect(prepareParamsForFooterGeoInfo(params)).toEqual({
            category: 'cars',
            section: 'new',
            catalog_filter: [
                {
                    mark: 'audi',
                    model: 'a6',
                },
            ],
        });
    });
});
