/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */
jest.mock('auto-core/react/dataDomain/card/actions/actualize', () => {
    return jest.fn(() => (dispatch: any) => {
        dispatch({ type: 'MOCK_ACTUALIZE' });
        return Promise.resolve();
    });
});

import type { ShallowWrapper } from 'enzyme';
import 'jest-enzyme';
import React from 'react';
import { shallow } from 'enzyme';

import type { TOfferMock } from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';
import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import actualize from 'auto-core/react/dataDomain/card/actions/actualize';

import ButtonActualizeDesktop from './ButtonActualizeMobile';

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

describe('при клике на вопросик', () => {
    let wrapper: ShallowWrapper;
    beforeEach(() => {
        wrapper = shallow(
            <ButtonActualizeDesktop offer={ offer.value() }/>,
            { context: { ...contextMock, store } },
        ).dive();

        wrapper.find('.ButtonActualizeMobile__icon').simulate('click', new Event('click'));
    });

    it('показать модал с информацией об актуализации', () => {
        expect(wrapper.find('ModalDialogActualizeOffer')).toHaveProp('visible', true);
        expect(wrapper.find('ModalDialogActualizeOffer')).toHaveProp('success', false);
    });
});

describe('при клике', () => {
    let wrapper: ShallowWrapper;
    beforeEach(async() => {
        wrapper = shallow(
            <ButtonActualizeDesktop offer={ offer.value() }/>,
            { context: { ...contextMock, store } },
        ).dive();

        wrapper.find('ButtonActualizeColored').simulate('click');

        // промис
        await new Promise(resolve => setTimeout(resolve, 100));
    });

    it('отправить метрику "clicks,actualize"', () => {
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'clicks', 'actualize' ]);
    });

    it('вызвать экшен актуализации', () => {
        expect(actualize).toHaveBeenCalledWith({ offer: offer.value() });
    });

    it('показать модал об успешной актуализации', () => {
        expect(wrapper.find('ModalDialogActualizeOffer')).toHaveProp('visible', true);
        expect(wrapper.find('ModalDialogActualizeOffer')).toHaveProp('success', true);
    });
});
