import React from 'react';
import { shallow } from 'enzyme';

import { CardTypeInfo_CardType } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/garage/garage_api_model';
import { OfferStatus, AdditionalInfo_ProvenOwnerStatus } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';

import Link from 'auto-core/react/components/islands/Link/Link';
import { nbsp } from 'auto-core/react/lib/html-entities';

import garageCardMock from 'auto-core/models/garageCard/mocks/mockChain';

const Context = createContextProvider(contextMock);

import GarageCardOfferLink from './GarageCardOfferLink';

describe('GarageCardOfferLink', () => {
    it('рендерит ссылку c текстом "Продать", так как для машины нет объявления', () => {
        const card = garageCardMock
            .withCardType(CardTypeInfo_CardType.CURRENT_CAR)
            .value();
        const wrapper = shallow(
            <GarageCardOfferLink
                garageCard={ card }
            />,
        );

        expect(wrapper.find(Link)).toExist();
        expect(wrapper.children().text()).toEqual('Продать');
        expect(wrapper.prop('metrika')).toEqual('sell');
    });

    it('рендерит ссылку с текстом "Продать за 200000", так как для машины есть активное объявление', () => {
        const card = garageCardMock
            .withCardType(CardTypeInfo_CardType.CURRENT_CAR)
            .withOfferInfo()
            .value();

        const wrapper = shallow(
            <Context>
                <GarageCardOfferLink
                    garageCard={ card }
                />
            </Context>,
        );

        expect(wrapper.dive().find(Link)).toExist();
        expect(wrapper.dive().children().text()).toEqual(`Продаётся за 200${ nbsp }000${ nbsp }₽`);
        expect(wrapper.dive().prop('metrika')).toEqual('sale,price,link');
    });

    it('рендерит ссылку с текстом "Продать", так как для машины есть неактивное объявление, непубличная карточка', () => {
        const OFFER_INFO = {
            offer_id: '1114113983-2db403c7',
            status: OfferStatus.STATUS_UNKNOWN,
            price: 200000,
            proven_owner_status: AdditionalInfo_ProvenOwnerStatus.OK,
        };

        const card = garageCardMock
            .withCardType(CardTypeInfo_CardType.CURRENT_CAR)
            .withOfferInfo(OFFER_INFO)
            .value();

        card.is_shared_view = false;

        const wrapper = shallow(
            <Context>
                <GarageCardOfferLink
                    garageCard={ card }
                />
            </Context>,
        );

        expect(wrapper.dive().find(Link)).toExist();
        expect(wrapper.dive().children().text()).toEqual('Продать');
        expect(wrapper.dive().prop('metrika')).toEqual('sale,link');
    });
});
