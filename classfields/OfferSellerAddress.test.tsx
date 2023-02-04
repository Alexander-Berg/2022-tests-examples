import React from 'react';
import { shallow } from 'enzyme';
import { shallowToJson } from 'enzyme-to-json';

import type { Seller, Offer } from 'auto-core/types/proto/auto/api/api_offer_model';

import OfferSellerAddress from './OfferSellerAddress';

it('должен отрендерить адрес', () => {
    const offer: Partial<Offer> = {
        seller: {
            location: {
                address: 'Льва Толстого, 16',
            },
        } as Partial<Seller> as Seller,
    };

    const wrapper = shallow(
        <OfferSellerAddress offer={ offer as Offer }/>,
    );

    expect(shallowToJson(wrapper)).toMatchSnapshot();
});

it('должен отрендерить адрес и регион', () => {
    const offer: Partial<Offer> = {
        seller: {
            location: {
                address: 'Льва Толстого, 16',
                region_info: {
                    id: '213',
                    name: 'Москва',
                    latitude: 55.753215,
                    longitude: 37.622504,
                },
            },
        } as Partial<Seller> as Seller,
    };

    const wrapper = shallow(
        <OfferSellerAddress offer={ offer as Offer } renderRegionName={ true }/>,
    );

    expect(shallowToJson(wrapper)).toMatchSnapshot();
});
