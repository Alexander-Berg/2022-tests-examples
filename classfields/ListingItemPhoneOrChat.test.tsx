import React from 'react';
import { shallow } from 'enzyme';

import type { Salon } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';
import 'jest-enzyme';

import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import ListingItemPhoneOrChat from './ListingItemPhoneOrChat';

it('должен отрисовать имя и тип для частника', () => {
    const offer = cloneOfferWithHelpers({})
        .withSellerTypePrivate()
        .withSellerName('Имя владельца');

    const wrapper = shallow(
        <ListingItemPhoneOrChat
            offer={ offer.value() }
        />,
    );
    expect(wrapper.find('.ListingItemPhoneOrChat__name').text()).toBe('Имя владельца');
    expect(wrapper.find('.ListingItemPhoneOrChat__type').text()).toBe('Частное лицо');
    expect(wrapper.find('ModalSellerInfo')).not.toExist();
});

it('должен отрисовать тип для частника, если имя не указано', () => {
    const offer = cloneOfferWithHelpers({})
        .withSellerTypePrivate()
        .withSellerName('id12345');

    const wrapper = shallow(
        <ListingItemPhoneOrChat
            offer={ offer.value() }
        />,
    );

    expect(wrapper.find('.ListingItemPhoneOrChat__name').text()).toBe('Частное лицо');
    expect(wrapper.find('.ListingItemPhoneOrChat__type')).not.toExist();

});

it('должен отрисовать имя и тип для дилера', () => {
    const offer = cloneOfferWithHelpers({})
        .withSalon({ is_oficial: true } as Salon)
        .withSection('new')
        .withSellerTypeCommercial()
        .withSellerName('Имя владельца');
    const wrapper = shallow(
        <ListingItemPhoneOrChat
            offer={ offer.value() }
        />,
    );

    expect(wrapper.find('.ListingItemPhoneOrChat__name').text()).toBe('Имя владельца');
    expect(wrapper.find('Link').childAt(0).text()).toBe('Контакты');
    expect(wrapper.find('ModalSellerInfo')).toExist();
});

it('должен отрисовать кнопку позвонить', () => {
    const offer = cloneOfferWithHelpers({});

    const wrapper = shallow(
        <ListingItemPhoneOrChat
            offer={ offer.value() }
        />,
    );
    expect(wrapper.find('Connect(OfferShowPhoneButton)')).toExist();
    expect(wrapper.find('Connect(OpenChatByOffer)')).not.toExist();
});

it('должен отрисовать кнопку написать', () => {
    const offer = cloneOfferWithHelpers({}).withChatOnly();

    const wrapper = shallow(
        <ListingItemPhoneOrChat
            offer={ offer.value() }
        />,
    );
    expect(wrapper.find('Connect(OfferShowPhoneButton)')).not.toExist();
    expect(wrapper.find('Connect(OpenChatByOffer)')).toExist();
});
