import 'jest-enzyme';
import React from 'react';
import { shallow } from 'enzyme';
import { render as renderUsingTestingLibrary } from '@testing-library/react';
import { mockAllIsIntersecting } from 'react-intersection-observer/test-utils';

import type { Location } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';
import { OfferStatus, SellerType } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';
import type { TOfferMock } from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';

import type { Seller, Offer } from 'auto-core/types/proto/auto/api/api_offer_model';

import type { MobileAppState } from 'www-mobile/react/MobileAppState';

import OfferSellerInfo from './OfferSellerInfo';

let store: MobileAppState;
beforeEach(() => {
    store = mockStore({
        bunker: {},
        dealerCallback: { isVisible: false },
        cookie: {},
    }) as Partial<MobileAppState> as MobileAppState;
});

describe('Компонент "такси"', () => {
    let offer: TOfferMock;
    beforeEach(() => {
        offer = cloneOfferWithHelpers({
            seller: {
                location: {
                    coord: { latitude: 1, longitude: 1 },
                } as Partial<Location> as Location,
            } as Partial<Seller> as Seller,
        })
            .withCategory('cars')
            .withStatus(OfferStatus.ACTIVE);
    });

    it('должен отрендерить для невладельца, если есть координаты', async() => {
        const wrapper = shallow(
            <OfferSellerInfo offer={ offer.value() }/>,
            { context: { ...contextMock, store } },
        );

        expect(wrapper.find('CardTaxiLink')).toExist();
    });

    it('не должен отрендерить для владельнца', async() => {
        offer = offer.withIsOwner(true);

        const wrapper = shallow(
            <OfferSellerInfo offer={ offer.value() }/>,
            { context: { ...contextMock, store } },
        );

        expect(wrapper.find('CardTaxiLink')).not.toExist();
    });
});

it('отправит метрику при наличии ссылки на перекупа', () => {
    const OFFER: Partial<Offer> = {
        seller_type: SellerType.PRIVATE,
        seller: {
            name: 'Владимир',
            location: {
                address: 'метро проспект мира',
                region_info: {
                    id: '213',
                    name: 'Москва',
                    latitude: 55.753215,
                    longitude: 37.622504,
                    genitive: '',
                    dative: '',
                    accusative: '',
                    instrumental: '',
                    prepositional: '',
                    preposition: '',
                    sub_title: '',
                    supports_geo_radius: true,
                    default_radius: 200,
                    children: [],
                    parent_ids: [],
                },
            } as Partial<Location> as Location,
            redirect_phones: false,
        } as Seller,
    };
    const publicUserInfo = {
        alias: 'Hulio',
        offers_stats_by_category: {
            ALL: { active_offers_count: 4, inactive_offers_count: 0 },
            CARS: { active_offers_count: 4, inactive_offers_count: 0 },
            MOTO: { active_offers_count: 0, inactive_offers_count: 0 },
            TRUCKS: { active_offers_count: 0, inactive_offers_count: 0 },
        },
        registration_date: '',
    };

    const offer = cloneOfferWithHelpers(OFFER)
        .withEncryptedUserId('id123')
        .value();

    const Context = createContextProvider(contextMock);

    renderUsingTestingLibrary(
        <Context>
            <OfferSellerInfo offer={ offer } publicUserInfo={ publicUserInfo }/>
        </Context>,
    );

    mockAllIsIntersecting(true);

    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'reseller_public', 'link-show' ]);
});
