const _ = require('lodash');
const mockOffer = require('autoru-frontend/mockData/responses/offer.mock').offer;
const getFirstOfferDealerCount = require('auto-core/react/lib/offer/getFirstOfferDealerCount').default;

it('Должен вернуть 0, если передали groupping_info = {}', () => {
    const offer = _.cloneDeep(mockOffer);
    offer.groupping_info = {};
    expect(getFirstOfferDealerCount(offer)).toEqual(0);
});

it('Должен вернуть число офферов, если передали groupping_info', () => {
    const offer = _.cloneDeep(mockOffer);
    offer.salon = {
        dealer_id: '20134432',
    };
    offer.groupping_info = {
        dealers: [
            {
                salon: {
                    dealer_id: '20134432',
                },
                offers_count: 103,
            },
        ],
    };
    expect(getFirstOfferDealerCount(offer)).toEqual(103);
});
