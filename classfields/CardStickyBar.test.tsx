/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import React from 'react';
import { shallow } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import cardStateMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';

import CardStickyBar from './CardStickyBar';

const defaultProps = {
    offer: cloneOfferWithHelpers(cardStateMock).withIsOwner(false).withPhones([]).withSeller({ chats_enabled: true }).value(),
    pageParams: {},
};

describe('должен уходить source="sidebar"', () => {
    const store = mockStore();
    const wrapper = shallow(
        <CardStickyBar
            { ...defaultProps }
        />, { context: { ...contextMock, store } },
    ).dive();

    it('в OfferPhone', () => {
        const component = wrapper.find('Connect(OfferPhone)');
        expect(component.prop('source')).toEqual('sidebar');
    });

    it('в ButtonFavorite', () => {
        const component = wrapper.find('Connect(ButtonFavorite)');
        expect(component.prop('source')).toEqual('sidebar');
    });

    it('в ButtonCompare', () => {
        const component = wrapper.find('Connect(ButtonCompare)');
        expect(component.prop('source')).toEqual('sidebar');
    });
});
