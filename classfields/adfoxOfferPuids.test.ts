jest.mock('auto-core/lib/ads/getAdfoxPricePuid', () => {
    return () => 'PRICE_RANGE_MOCK';
});

import { OfferStatus, PtsStatus } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';

import type { TOfferMock } from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import type { Offer } from 'auto-core/types/proto/auto/api/api_offer_model';

import adfoxCarPuids from './adfoxOfferPuids';

let offer: TOfferMock;
beforeEach(() => {
    offer = cloneOfferWithHelpers({
        documents: {},
        id: '123',
        state: {},
        vehicle_info: {},
    } as Partial<Offer>);
});

describe('puid2 (марка)', () => {
    it('должен вернуть код марки', () => {
        offer = offer.withMarkInfo({ code: 'FORD', numeric_id: '3152' });
        expect(adfoxCarPuids(offer.value())).toHaveProperty('puid2', '3152');
    });
});

describe('puid3 (модель)', () => {
    it('должен вернуть код модели', () => {
        offer = offer.withModelInfo({ code: 'FOCUS_ST' });
        expect(adfoxCarPuids(offer.value())).toHaveProperty('puid3', 'FOCUS_ST');
    });
});

describe('puid4 (год)', () => {
    it('должен вернуть год', () => {
        offer = offer.withYear(2019);
        expect(adfoxCarPuids(offer.value())).toHaveProperty('puid4', '2019');
    });
});

describe('puid5 (цена(', () => {
    it('должен вернуть пустую строку, если нет цены', () => {
        expect(adfoxCarPuids(offer.value())).toHaveProperty('puid5', '');
    });

    it('должен вернуть диапазон цен', () => {
        offer = offer.withPrice(100000);
        expect(adfoxCarPuids(offer.value())).toHaveProperty('puid5', 'PRICE_RANGE_MOCK');
    });
});

describe('puid11 (статус объявления)', () => {
    it('должен вернуть SALE, если объявление активно', () => {
        offer = offer.withStatus(OfferStatus.ACTIVE);
        expect(adfoxCarPuids(offer.value())).toHaveProperty('puid11', 'SALE');
    });

    it('должен вернуть SOLD, если объявление не активно', () => {
        offer = offer.withStatus(OfferStatus.INACTIVE);
        expect(adfoxCarPuids(offer.value())).toHaveProperty('puid11', 'SOLD');
    });
});

describe('puid12 (тип продавца)', () => {
    it('должен вернуть private, если объявление от частника', () => {
        offer = offer.withSellerTypePrivate();
        expect(adfoxCarPuids(offer.value())).toHaveProperty('puid12', 'private');
    });

    it('должен вернуть dealer, если объявление от официального салона', () => {
        offer = offer.withSellerTypeCommercial().withOfficialSalon();
        expect(adfoxCarPuids(offer.value())).toHaveProperty('puid12', 'dealer');
    });

    it('должен вернуть other, если объявление от неофициального салона', () => {
        offer = offer.withSellerTypeCommercial().withSalon({ is_oficial: false });
        expect(adfoxCarPuids(offer.value())).toHaveProperty('puid12', 'other');
    });
});

describe('puid15 (битый)', () => {
    it('должен вернуть 1, если машина не битая', () => {
        offer = offer.withIsBeaten(false);
        expect(adfoxCarPuids(offer.value())).toHaveProperty('puid15', '1');
    });

    it('должен вернуть 2, если машина битая', () => {
        offer = offer.withIsBeaten(true);
        expect(adfoxCarPuids(offer.value())).toHaveProperty('puid15', '2');
    });
});

describe('puid16 (ПТС)', () => {
    it('должен вернуть 0, если ПТС нет данных', () => {
        expect(adfoxCarPuids(offer.value())).toHaveProperty('puid16', '0');
    });

    it('должен вернуть 1, если ПТС оригинал', () => {
        offer = offer.withPts(PtsStatus.ORIGINAL);
        expect(adfoxCarPuids(offer.value())).toHaveProperty('puid16', '1');
    });

    it('должен вернуть 2, если ПТС дубликат', () => {
        offer = offer.withPts(PtsStatus.DUPLICATE);
        expect(adfoxCarPuids(offer.value())).toHaveProperty('puid16', '2');
    });
});

describe('puid17 (вендор)', () => {
    it('должен вернуть вендора', () => {
        offer = offer.withVehicleInfo({ vendor: 'VENDOR' });
        expect(adfoxCarPuids(offer.value())).toHaveProperty('puid17', 'VENDOR');
    });

    it('должен вернуть пустую строку, если нет вендора', () => {
        expect(adfoxCarPuids(offer.value())).toHaveProperty('puid17', '');
    });
});

describe('puid20 (количество владельцев)', () => {
    it('должен вернуть количество владельцев', () => {
        offer = offer.withDocuments({ owners_number: 2 });
        expect(adfoxCarPuids(offer.value())).toHaveProperty('puid20', '2');
    });

    it('должен вернуть пустую строку, если нет данных', () => {
        expect(adfoxCarPuids(offer.value())).toHaveProperty('puid20', '');
    });
});
