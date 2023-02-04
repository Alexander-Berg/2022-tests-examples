import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import cardMock from 'auto-core/react/dataDomain/card/mocks/card.cars.mock';

import vipPackageData from 'auto-core/data/vip_package.json';
import { TOfferVas } from 'auto-core/types/proto/auto/api/api_offer_model';

import hasVipPackageProposal from './hasVipPackageProposal';

describe('вернет true', () => {
    it('если категория cars и цена оффера выше переданного значения', () => {
        const minPrice = 800000;
        const offer = cloneOfferWithHelpers(cardMock)
            .withCategory('cars')
            .withPrice(minPrice)
            .value();

        expect(hasVipPackageProposal(offer, minPrice)).toBe(true);
    });

    it('если категория cars, минимальное значение не передано и цена оффера выше значения в словаре', () => {
        const offer = cloneOfferWithHelpers(cardMock)
            .withCategory('cars')
            .withPrice(vipPackageData.threshold)
            .withCustomVas({ service: TOfferVas.VIP, recommendation_priority: 0 })
            .value();

        expect(hasVipPackageProposal(offer)).toBe(true);
    });

    it('если категория cars и с бэка пришел флаг с рекомендацией', () => {
        const offer = cloneOfferWithHelpers(cardMock)
            .withCategory('cars')
            .withPrice(vipPackageData.threshold - 1)
            .withCustomVas({ service: TOfferVas.VIP, recommendation_priority: 10 })
            .value();

        expect(hasVipPackageProposal(offer)).toBe(true);
    });
});

describe('вернет false', () => {
    it('если категория не cars', () => {
        const offer = cloneOfferWithHelpers(cardMock)
            .withCategory('moto')
            .withPrice(vipPackageData.threshold)
            .value();

        expect(hasVipPackageProposal(offer)).toBe(false);
    });

    it('если цена оффера меньше переданного значения', () => {
        const minPrice = 800000;
        const offer = cloneOfferWithHelpers(cardMock)
            .withCategory('cars')
            .withPrice(minPrice - 1)
            .withCustomVas({ service: TOfferVas.VIP, recommendation_priority: 0 })
            .value();

        expect(hasVipPackageProposal(offer, minPrice)).toBe(false);
    });

    it('если минимальное значение не передано и цена оффера ниже значения в словаре', () => {
        const offer = cloneOfferWithHelpers(cardMock)
            .withCategory('cars')
            .withPrice(vipPackageData.threshold - 1)
            .withCustomVas({ service: TOfferVas.VIP, recommendation_priority: 0 })
            .value();

        expect(hasVipPackageProposal(offer)).toBe(false);
    });
});
