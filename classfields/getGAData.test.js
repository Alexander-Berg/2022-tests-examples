const mockOffer = require('autoru-frontend/mockData/responses/offer.mock').offer;

const getGAData = require('./getGAData');

it('должен вернуть корректные данные для счетчика GoogleAnalytics', () => {
    expect(getGAData(mockOffer)).toEqual('mark-volkswagen_model-amarok_seller-user_status-used_category-cars');
});
