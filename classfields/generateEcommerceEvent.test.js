const MockDate = require('mockdate');
const mockOffer = require('autoru-frontend/mockData/responses/offer.mock').offer;

const generateEcommerceEvent = require('./generateEcommerceEvent');

const ecommerceMock = require('autoru-frontend/mockData/bunker/common/metrics.json').ecommerce;

it('должен вернуть корректные данные для события просмотра страницы для Метрики', () => {
    expect(generateEcommerceEvent('pageview', ecommerceMock, mockOffer)).toMatchSnapshot();
});

it('должен вернуть корректные данные для события просмотра телефона для Метрики', () => {
    MockDate.set('2019-02-26T13:13:13.000+0300');
    expect(generateEcommerceEvent('phoneview', ecommerceMock, mockOffer)).toMatchSnapshot();
});

it('при отсутствии стоимости цели не должен генерировать событие для Метрики', () => {
    expect(generateEcommerceEvent('phoneview', null, mockOffer)).toBeUndefined();
});
