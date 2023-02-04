/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import React from 'react';
import { Provider } from 'react-redux';
import { mount } from 'enzyme';
import { shallowToJson } from 'enzyme-to-json';
import type { ShallowWrapper } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import mockStore from 'autoru-frontend/mocks/mockStore';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import creditProductMock from 'auto-core/react/dataDomain/credit/mocks/creditProduct.mockchain';
import bankMock from 'auto-core/react/dataDomain/credit/mocks/bank.mockchain';
import card from 'auto-core/react/dataDomain/card/mocks/card.cars.mock';

import { BankID } from 'auto-core/types/TCreditBroker';

const Context = createContextProvider(contextMock);

const product = creditProductMock()
    .withBankID(BankID.TINKOFF)
    .withID('test-2')
    .value();

const bank = bankMock().fromBankID(BankID.TINKOFF).value();

const offer = cloneOfferWithHelpers(card)
    .withIsOwner(false)
    .withCreditPrecondition()
    .withBaseDate('2019-08-16')
    .value();

import CreditDynamicBanner from './CreditDynamicBanner';

it('рисует ссылку, когда все данные есть', () => {
    const store = mockStore({
        credit: {
            dynamicBanner: {
                bank,
                creditProduct: product,
            },
        },
    });

    const wrapper = mount(
        <Context>
            <Provider store={ store }>
                <CreditDynamicBanner offer={ offer }/>
            </Provider>
        </Context>,
    );

    const link = wrapper.find('CreditDynamicBanner').find('Link') as unknown as ShallowWrapper;

    expect(shallowToJson(link)).not.toBeNull();
});

it('не рисуется, если нет кредитных продуктов или банков', () => {
    const store = mockStore({
        credit: {},
    });

    const wrapper = mount(
        <Context>
            <Provider store={ store }>
                <CreditDynamicBanner offer={ offer }/>
            </Provider>
        </Context>,
    );

    const link = wrapper.find('CreditDynamicBanner').find('Link') as unknown as ShallowWrapper;

    expect(shallowToJson(link)).toBeNull();
});

it('не рисуется, если у оффера есть интеграция е-кредита', () => {
    const store = mockStore({
        credit: {},
    });

    const offerWithEcredit = cloneOfferWithHelpers(card)
        .withIsOwner(false)
        .withCreditPrecondition()
        .withEcreditPrecondition()
        .value();

    const wrapper = mount(
        <Context>
            <Provider store={ store }>
                <CreditDynamicBanner offer={ offerWithEcredit }/>
            </Provider>
        </Context>,
    );

    const link = wrapper.find('CreditDynamicBanner').find('Link') as unknown as ShallowWrapper;

    expect(shallowToJson(link)).toBeNull();
});
