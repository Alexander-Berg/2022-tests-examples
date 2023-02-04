import versusMock from '../mock';

import getRequestData from './getRequestData';

const versus = versusMock
    .withModelId({ number: 0, id: '21738487_22128725_21738491' })
    .value();

it('должен правильно сформировать данные для запроса, если передали новое поколение', () => {
    const params = { oldVersusModelId: '21738487_22128725_21738491_0', generation: '123' };

    expect(getRequestData(versus, params)).toMatchObject({
        catalog_filter: [
            {
                generation: '123',
                mark: 'FORD',
                model: 'ECOSPORT',
            },
            {
                complectation: '22128725',
                configuration: '21738487',
                generation: '21738448',
                mark: 'KIA',
                model: 'RIO',
                tech_param: '21738490',
            },
        ],
    });
});

it('должен правильно сформировать данные для запроса, если передали новую модель', () => {
    const params = {
        oldVersusModelId: '21738487_22128725_21738491_0',
        configuration: '123',
        complectation: '456',
        techParam: '789',
    };

    expect(getRequestData(versus, params)).toMatchObject({
        catalog_filter: [
            {
                complectation: '456',
                configuration: '123',
                generation: '20104320',
                mark: 'FORD',
                model: 'ECOSPORT',
                tech_param: '789',
            },
            {
                complectation: '22128725',
                configuration: '21738487',
                generation: '21738448',
                mark: 'KIA',
                model: 'RIO',
                tech_param: '21738490',
            },
        ],
    });
});

it('должен вернуть данные по текущим моделям, если не передали не newModelId, ни generation', () => {
    const params = { oldVersusModelId: '21738487_22128725_21738491' };

    expect(getRequestData(versus, params)).toMatchObject({
        catalog_filter: [
            {
                complectation: '22128725',
                configuration: '21738487',
                generation: '20104320',
                mark: 'FORD',
                model: 'ECOSPORT',
                tech_param: '21738490',
            },
            {
                complectation: '22128725',
                configuration: '21738487',
                generation: '21738448',
                mark: 'KIA',
                model: 'RIO',
                tech_param: '21738490',
            },
        ],
    });
});
