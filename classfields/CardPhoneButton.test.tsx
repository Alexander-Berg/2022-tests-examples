import React from 'react';
import { shallow } from 'enzyme';

import type { Seller } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';

import { TMetrikaShowPhoneSource } from 'auto-core/react/dataDomain/offerPhones/lib/enums';

import type { Offer } from 'auto-core/types/proto/auto/api/api_offer_model';

import CardPhoneButton from './CardPhoneButton';

let offer: Offer;
beforeEach(() => {
    offer = {
        seller: {
            phones_pending: false,
        } as Partial<Seller> as Seller,
    } as Partial<Offer> as Offer;
});

it('правильно формирует проп для метрики показа телефона', () => {
    const tree = shallow(
        <CardPhoneButton offer={ offer } metrikaSource={ TMetrikaShowPhoneSource.BUTTON }/>,
    );

    expect(tree.find('Connect(OfferShowPhoneButton)').prop('metricsData')).toMatchSnapshot();
});
