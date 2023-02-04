import 'jest-enzyme';
import React from 'react';
import { shallow } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';
import type { TOfferMock } from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

jest.mock('auto-core/react/dataDomain/card/actions/actualize', () => {
    return jest.fn(() => ({ type: 'MOCK_ACTUALIZE' }));
});

import actualize from 'auto-core/react/dataDomain/card/actions/actualize';

import ButtonActualizeDesktop from './ButtonActualizeDesktop';

let offer: TOfferMock;
let store: any;
beforeEach(() => {
    offer = cloneOfferWithHelpers({})
        .withIsOwner(true)
        .withSellerTypePrivate();
    store = mockStore({});
});

describe('условия', () => {
    it('должен показываться только для владельца частница', () => {
        const wrapper = shallow(
            <ButtonActualizeDesktop offer={ offer.value() }/>,
            { context: { ...contextMock, store } },
        ).dive();

        expect(wrapper).not.toBeEmptyRender();
    });

    it('не должен показываться для невладельца', () => {
        offer.withIsOwner(false);

        const wrapper = shallow(
            <ButtonActualizeDesktop offer={ offer.value() }/>,
            { context: { ...contextMock, store } },
        ).dive();

        expect(wrapper).toBeEmptyRender();
    });

    it('не должен показываться для владельца салонов', () => {
        offer.withSellerTypeCommercial();

        const wrapper = shallow(
            <ButtonActualizeDesktop offer={ offer.value() }/>,
            { context: { ...contextMock, store } },
        ).dive();

        expect(wrapper).toBeEmptyRender();
    });
});

describe('при клике', () => {
    beforeEach(() => {
        const wrapper = shallow(
            <ButtonActualizeDesktop offer={ offer.value() }/>,
            { context: { ...contextMock, store } },
        ).dive();

        wrapper.find('ButtonActualizeColored').simulate('click');
    });

    it('отправить метрику "clicks,actualize"', () => {
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'clicks', 'actualize' ]);
    });

    it('вызвать экшен актуализации', () => {
        expect(actualize).toHaveBeenCalledWith({ offer: offer.value() });
    });
});
