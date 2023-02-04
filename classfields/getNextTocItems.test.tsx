import React from 'react';
import { shallow } from 'enzyme';

import {
    ProvenOwnerState_ProvenOwnerStatus,
    CardTypeInfo_CardType,
} from '@vertis/schema-registry/ts-types-snake/auto/api/vin/garage/garage_api_model';

import { ID as GarageCardVinReportID } from 'auto-core/react/components/common/GarageCardVinReport/GarageCardVinReport';
import garagePromoMock from 'auto-core/react/dataDomain/garagePromoAll/mocks';

import garageCardMock from 'auto-core/models/garageCard/mocks/mockChain';
import type { Card } from 'auto-core/types/proto/auto/api/vin/garage/garage_api_model';

import GarageCardMobileTocPromoItem from '../GarageCardMobileTocPromoItem/GarageCardMobileTocPromoItem';
import GarageCardMobileTocItem from '../GarageCardMobileTocItem/GarageCardMobileTocItem';
import GarageCardMobileOwnerCheckItem from '../GarageCardMobileOwnerCheckItem';

import getNextTocItems from './getNextTocItems';

const garagePromos = [
    garagePromoMock.withType('SUPER_PROMO').value(),
    garagePromoMock.withType('SUPER_PROMO').withId('1').value(),
    garagePromoMock.withType('SUPER_PROMO').withId('2').value(),
];
const TOTAL_ITEMS = 10;

describe('getNextTocItems', () => {
    it('возвращает массив компонентов с промо для обычной карточки гаража, промо вставляется на нечетные индексы массива', () => {
        const card = garageCardMock.withPartnerPromos().withRichReviews().value();
        card.proven_owner_state = {
            comment: '',
            task_key: '',
            status: ProvenOwnerState_ProvenOwnerStatus.OK,
        };

        const actual = getNextTocItems({ card, lentaTotalItemsCount: 0, garagePromos });
        const wrapper = shallow(<div>{ [ actual ] }</div>);

        expect(wrapper.childAt(0).type()).toBe(GarageCardMobileOwnerCheckItem);
        expect(wrapper.childAt(1).type()).toBe(GarageCardMobileTocPromoItem);
        expect(wrapper.childAt(2).type()).toBe(GarageCardMobileTocItem);
        expect(wrapper.childAt(3).type()).toBe(GarageCardMobileTocPromoItem);
        expect(wrapper.childAt(4).type()).toBe(GarageCardMobileTocItem);

        // Должно быть только две промки, остальное по ссылке на "Все акции"
        expect(wrapper.find(GarageCardMobileTocPromoItem)).toHaveLength(2);
    });

    it('возвращает массив компонентов в правильном порядке и без промо для карточки гаража машины мечты', () => {
        const card = garageCardMock
            .withPartnerPromos()
            .withRichReviews()
            .withPriceStats()
            .withListingOffers({
                listing_offers_count: 15,
            } as Card['listing_offers'])
            .withCardType(CardTypeInfo_CardType.DREAM_CAR)
            .value();

        const actual = getNextTocItems({ card, garagePromos, lentaTotalItemsCount: TOTAL_ITEMS });
        const wrapper = shallow(<div>{ [ actual ] }</div>);

        expect(wrapper.childAt(0).prop('type')).toBe('on-sale');
        expect(wrapper.childAt(1).prop('type')).toBe('articles-reviews');
        expect(wrapper.childAt(2).prop('type')).toBe('price_stats');
        expect(wrapper.childAt(3).prop('type')).toBe('reviews');
        expect(wrapper.childAt(4).prop('type')).toBe('regular-promos');
        expect(wrapper.childAt(5).prop('type')).toBe('cheapening_graph');
        expect(wrapper.find(GarageCardMobileTocPromoItem).exists()).toBeFalsy();
        expect(wrapper.find(GarageCardMobileTocItem).exists()).toBeTruthy();
    });

    it('возвращает массив компонентов в правильном порядке и без промо для карточки бывшей машины', () => {
        const card = garageCardMock
            .withCardType(CardTypeInfo_CardType.EX_CAR)
            .withPartnerPromos()
            .withReport()
            .withRichReviews()
            .value();

        const actual = getNextTocItems({ card, garagePromos, lentaTotalItemsCount: TOTAL_ITEMS });
        const wrapper = shallow(<div>{ [ actual ] }</div>);

        expect(wrapper.childAt(0).prop('type')).toBe(GarageCardVinReportID);
        expect(wrapper.childAt(1).prop('type')).toBe('reviews');
        expect(wrapper.childAt(2).prop('type')).toBe('articles-reviews');
        expect(wrapper.find(GarageCardMobileTocPromoItem).exists()).toBeFalsy();
        expect(wrapper.find(GarageCardMobileTocItem).exists()).toBeTruthy();
    });
});
