const getDataForEngineLink = require('auto-core/react/lib/offer/getDataForEngineLink');
const mockOffer = require('autoru-frontend/mockData/responses/offer.mock');

it('Должен вернуть нормальное значение для оффера легковой', () => {
    expect(getDataForEngineLink(mockOffer.offer)).toEqual({ displacement: '2.0 л', engineType: 'Дизель', power: '180 л.с.' },
    );
});

it('Должен вернуть нормальное значение для оффера грузовой', () => {
    expect(getDataForEngineLink(mockOffer.offerTruck)).toEqual({ displacement: '3.0 л', engineType: 'Дизель', power: '124 л.с.' },
    );
});

it('Должен вернуть пустые строки, если данных нет', () => {
    expect(getDataForEngineLink({})).toEqual({ displacement: '', engineType: '', power: '' });
});
