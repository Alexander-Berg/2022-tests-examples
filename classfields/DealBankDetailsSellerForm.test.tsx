/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import React from 'react';
import { Provider } from 'react-redux';
import { mount } from 'enzyme';

import { SellerStep, ParticipantType } from '@vertis/schema-registry/ts-types-snake/vertis/safe_deal/common';

import type { ThunkMockStore } from 'autoru-frontend/mocks/mockStore';
import mockStore from 'autoru-frontend/mocks/mockStore';
import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';

import safeDealMock from 'auto-core/react/dataDomain/safeDeal/mocks/safeDeal.mock';
import type { SafeDeal } from 'auto-core/react/dataDomain/safeDeal/types';

import convertApiDataToFormData from './convertApiDataToFormData';
import type { ReduxState } from './DealBankDetailsSellerForm';
import DealBankDetailsSellerForm from './DealBankDetailsSellerForm';

const Context = createContextProvider(contextMock);

let emptyStore: ThunkMockStore<ReduxState>;
beforeEach(() => {
    emptyStore = mockStore({
        safeDeal: {
            deal: {},
        } as SafeDeal,
    });
});

const store = mockStore({ safeDeal: safeDealMock });
const formData = convertApiDataToFormData((safeDealMock.deal?.party?.seller?.person_profile?.banking_entity || [])[0]);

describe('Форма банковских реквизитов продавца', () => {
    it('должен предзаполнять номера счета строкой "40817810", если его нет', async() => {
        const wrapper = mountComponent(emptyStore);

        wrapper.find('MaskedTextInput').find('input').simulate('focus');

        expect(wrapper.find('DealBankDetailsSellerForm').state('formData').account).toBe('40817810');
    });

    it('должен не изменять номера счета строкой, если его приходит с сервера', async() => {
        const wrapper = mountComponent(store);

        wrapper.find('MaskedTextInput').find('input').simulate('focus');

        expect(wrapper.find('DealBankDetailsSellerForm').state('formData').account).toBe(formData['account']);
    });

    it('должен показать ошибку, если длинна номера счета менее 20 символов', async() => {
        const wrapper = mountComponent(emptyStore);

        // Видимо из-за того, что input обернут в IMask не получается изменить
        // state вызывая input.simulate('change', { target: { value: '123' }}),
        // поэтому меняю state на прямую в форме;
        const form = wrapper.find('DealBankDetailsSellerForm');
        form.setState({ formData: { account: '40817810123457' } });

        expect(form.state('formErrors').account).toBe('Неверный номер счета');
    });

    it('должен показать ошибку, если длинна номера счета начинается не 40817810', async() => {
        const wrapper = mountComponent(emptyStore);

        // Видимо из-за того, что input обернут в IMask не получается изменить
        // state вызывая input.simulate('change', { target: { value: '123' }}),
        // поэтому меняю state на прямую в форме;;
        const form = wrapper.find('DealBankDetailsSellerForm');
        form.setState({ formData: { account: '50817810938160925982' } });

        expect(form.state('formErrors').account).toBe('Номер счета должен начинаться с 40817 810');
    });

    it('НЕ должен показать ошибку, если номер счета корректен', async() => {
        const wrapper = mountComponent(emptyStore);

        // Видимо из-за того, что input обернут в IMask не получается изменить
        // state вызывая input.simulate('change', { target: { value: '123' }}),
        // поэтому меняю state на прямую в форме;
        const form = wrapper.find('DealBankDetailsSellerForm');
        form.setState({ formData: { account: '40817810938160925982' } });

        expect(wrapper.find('DealBankDetailsSellerForm').state('formErrors').account).toBe(false);
    });

    it('должен показать ошибку, если длинна БИК менее 9 символов', async() => {
        const wrapper = mountComponent(emptyStore);

        wrapper.find({ placeholder: 'БИК банка получателя' }).find('input').simulate('change', { target: { value: '123' } });

        expect(wrapper.find('DealBankDetailsSellerForm').state('formErrors').bic).toBe('Неверный БИК');
    });

    it('НЕ должен показать ошибку, если длинна БИК корректна', async() => {
        const wrapper = mountComponent(emptyStore);

        wrapper.find({ placeholder: 'БИК банка получателя' }).find('input').simulate('change', { target: { value: '044525225' } });

        expect(wrapper.find('DealBankDetailsSellerForm').state('formErrors').bic).toBe(false);
    });
});

function mountComponent(store: ThunkMockStore<ReduxState>) {
    return mount(
        <Provider store={ store }>
            <Context>
                <DealBankDetailsSellerForm
                    dealId="123"
                    onUpdate={ () => Promise.resolve() }
                    dealPageType={ ParticipantType.SELLER.toLowerCase() }
                    role={ ParticipantType.SELLER }
                    step={ SellerStep.SELLER_INTRODUCING_ACCOUNT_DETAILS }
                    formStep="money"
                />
            </Context>
        </Provider>,
    );
}
