import type { TOfferMock } from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import type { Offer } from 'auto-core/types/proto/auto/api/api_offer_model';

import sendMetrikaForContact from './sendMetrikaForContact';

const events: { [key: string]: Array<string> } = {
    'new': [
        'CONTACT_CARS',
        'CONTACT_CARS_DESKTOP',
        'CONTACT_CARS_DEALER_NEW',
        'ym-show-contacts',
    ],
    dealerused: [
        'CONTACT_CARS',
        'CONTACT_CARS_DESKTOP',
        'CONTACT_CARS_DEALER_USED',
    ],
    dealerusedPrice1300: [
        'CONTACT_CARS',
        'CONTACT_CARS_DESKTOP',
        'CONTACT_CARS_DEALER_USED',
        'CONTACT_CARS_DEALER_USED_PRICE-1000-1999',
    ],
    dealerusedPrice4000: [
        'CONTACT_CARS',
        'CONTACT_CARS_DESKTOP',
        'CONTACT_CARS_DEALER_USED',
        'CONTACT_CARS_DEALER_USED_PRICE-2000-10000',
    ],
    used: [
        'CONTACT_CARS',
        'CONTACT_CARS_DESKTOP',
        'CONTACT_CARS_REGULAR',
    ],
};

const offers: { [key: string]: TOfferMock } = {
    'new': cloneOfferWithHelpers({})
        .withIsOwner(false)
        .withCategory('cars')
        .withSellerTypeCommercial()
        .withSection('new')
        .withSalon({ phone_callback_forbidden: false }),
    dealerused: cloneOfferWithHelpers({})
        .withIsOwner(false)
        .withCategory('cars')
        .withSellerTypeCommercial()
        .withSection('used')
        .withSalon({ phone_callback_forbidden: false }),
    dealerusedPrice1300: cloneOfferWithHelpers({})
        .withCategory('cars')
        .withSection('used')
        .withSalon({ phone_callback_forbidden: false })
        .withPrice(1300000),
    dealerusedPrice4000: cloneOfferWithHelpers({})
        .withCategory('cars')
        .withSection('used')
        .withSalon({ phone_callback_forbidden: false })
        .withPrice(4000000),
    used: cloneOfferWithHelpers({})
        .withIsOwner(false)
        .withCategory('cars')
        .withSection('used'),
};

const metrika = {
    reachGoal: jest.fn(),
    params: jest.fn(),
    sendPageEvent: jest.fn(),
    sendEcommerce: jest.fn(),
};

for (const section in offers) {
    const offer = offers[section];
    it('Должен отправить все цели для ' + section, () => {
        sendMetrikaForContact(offer as unknown as Offer, metrika, false);
        for (const event of events[section]) {
            expect(metrika.reachGoal).toHaveBeenCalledWith(event);
        }
    });
}
