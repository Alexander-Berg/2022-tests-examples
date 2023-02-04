const offerMock = require('autoru-frontend/mockData/responses/offer.mock');

const events = {
    'new': [
        [ 'PHONE_CARS2_ALL', null ],
        [ 'PHONE_CL_CARS2', null ],
        'CONTACT_CARS',
        'CONTACT_CARS_DESKTOP',
        'CONTACT_CARS_PHONE',
        'CONTACT_CARS_PHONE_NEW',
        'CONTACT_CARS_DEALER_NEW',
        'ym-show-contacts',
    ],
    dealerused: [
        [ 'PHONE_CARS2_ALL', null ],
        [ 'PHONE_CL_CARS2', null ],
        'CONTACT_CARS',
        'CONTACT_CARS_DESKTOP',
        'CONTACT_CARS_PHONE',
        'CONTACT_CARS_PHONE_USED',
        'ym-send-message',
        'CONTACT_CARS_DEALER_USED',
    ],
    dealerusedwithAuc: [
        [ 'PHONE_CARS2_ALL', null ],
        [ 'PHONE_CL_CARS2', null ],
        'CONTACT_CARS',
        'CONTACT_CARS_DESKTOP',
        'CONTACT_CARS_PHONE',
        'CONTACT_CARS_PHONE_USED',
        'ym-send-message',
        'CONTACT_CARS_DEALER_USED',
        'CONTACT_CARS_DEALER_USED_AUCTION',
    ],
    used: [
        [ 'PHONE_CARS2_ALL', null ],
        'PHONE_US_CARS2',
        'CONTACT_CARS',
        'CONTACT_CARS_DESKTOP',
        'CONTACT_CARS_PHONE',
        'CONTACT_CARS_PHONE_USED',
        'ym-send-message',
        'CONTACT_CARS_REGULAR',
    ],
};

const contextMock = require('autoru-frontend/mocks/contextMock').default;

const sendMetricsAfterPhonesFetch = require('./sendMetricsAfterPhonesFetch');
const source = 'button';
const offers = {
    'new': offerMock.offerNew,
    dealerused: { ...offerMock.offerNew, section: 'used' },
    dealerusedwithAuc: { ...offerMock.offerNew, section: 'used', tags: [ ...offerMock.offerNew.tags, 'with_lbu_auction_rank' ] },
    used: offerMock.offerUsed,
};

for (const section in offers) {
    const offer = offers[section];
    it('Должен отправить все цели для ' + section, () => {
        /* eslint-disable jest/no-conditional-expect */
        sendMetricsAfterPhonesFetch(offer, { source }, {}, contextMock.metrika);

        for (const event of events[section]) {
            if (typeof event === 'object') {
                expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith(...event);
            } else {
                expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith(event);
            }
        }
    });
}
