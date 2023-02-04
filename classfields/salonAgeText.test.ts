import MockDate from 'mockdate';

import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import type { Offer } from 'auto-core/types/proto/auto/api/api_offer_model';

import * as salonAgeText from './salonAgeText';

let offer: Partial<Offer>;
beforeEach(() => {
    offer = cloneOfferWithHelpers({})
        .withSalon({
            registration_date: '2019-02-27T15:48:38Z',
        })
        .value();
});

afterEach(() => {
    MockDate.reset();
});

describe('toShow()', () => {
    it('должен вернуть false, если нет даты регистрации', () => {
        expect(salonAgeText.toShow({} as Offer)).toEqual(false);
    });

    it('должен вернуть false, если салону меньше года', () => {
        MockDate.set('2020-02-26');
        expect(salonAgeText.toShow(offer as Offer)).toEqual(false);
    });

    it('должен вернуть true, если > 1 года', () => {
        MockDate.set('2020-02-28');
        expect(salonAgeText.toShow(offer as Offer)).toEqual(true);
    });
});

describe('getAge()', () => {
    it('должен вернуть 0, если нет даты регистрации', () => {
        expect(salonAgeText.getAge({} as Offer)).toEqual(0);
    });

    it('должен вернуть 0, если салону меньше года', () => {
        MockDate.set('2020-02-26');
        expect(salonAgeText.getAge(offer as Offer)).toEqual(0);
    });

    it('должен вернуть 1, если 1 год', () => {
        MockDate.set('2020-02-28');
        expect(salonAgeText.getAge(offer as Offer)).toEqual(1);
    });
});

describe('getText()', () => {
    it('должен вернуть "1 год"', () => {
        MockDate.set('2020-02-28');

        expect(salonAgeText.getText(offer as Offer)).toEqual('На Авто.ру 1 год');
    });

    it('должен отрендерить "5 лет"', () => {
        MockDate.set('2024-02-28');

        expect(salonAgeText.getText(offer as Offer)).toEqual('На Авто.ру 5 лет');
    });

    it('не должен ничего вернуть, если возраст меньше года', () => {
        MockDate.set('2020-02-26');

        expect(salonAgeText.getText(offer as Offer)).toEqual('');
    });
});
