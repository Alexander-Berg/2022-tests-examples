const mockOffer = require('autoru-frontend/mockData/responses/offer.mock').offer;

const generateParamsByOffer = require('./generateParamsByOffer');

it('должен вернуть корректные параметры для события просмотра страницы для Метрики', () => {
    expect(generateParamsByOffer(mockOffer, 'pageview')).toMatchSnapshot();
});

it('должен вернуть корректные параметры для события просмотра телефона для Метрики', () => {
    expect(generateParamsByOffer(mockOffer, 'phoneview')).toMatchSnapshot();
});
