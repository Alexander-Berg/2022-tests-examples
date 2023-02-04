import React from 'react';
import { shallow } from 'enzyme';
import { Provider } from 'react-redux';

import { BuyerStep } from '@vertis/schema-registry/ts-types-snake/vertis/safe_deal/common';

import mockStore from 'autoru-frontend/mocks/mockStore';

import DealBankDetails from './DealBankDetails';

const DEAL_NUMBER = '11727';

it('номер договора должен ОБЯЗАТЕЛЬНО содержать префикс БС в назначении платежа', () => {
    const tree = renderTree();
    const expectedText = `Оплата стоимости автомобиля по договору купли-продажи №БС${
        DEAL_NUMBER }. НДС не облагается`;

    tree.find('.DealBankDetails__button').simulate('click');

    const text = tree
        .find('Connect(DealBankDetailsItem)')
        .at(6).dive().dive()
        .find('.DealBankDetailsItem__value')
        .text();

    expect(text).toBe(expectedText);
});

function renderTree() {
    const details = {
        bankName: {
            title: 'Наименование банка',
            value: 'АО "Тинькофф Банк"',
            name: 'bank',
        },
        inn: {
            title: 'ИНН получателя',
            value: '7704340327',
            name: 'inn',
        },
        company: {
            title: 'Получатель',
            value: 'ООО "Яндекс.Вертикали"',
            name: 'company',
        },
        account: {
            title: 'Номер счета получателя',
            value: '40702810910000899488',
            name: 'account',
        },
        bik: {
            title: 'БИК банка получателя',
            value: '044525974',
            name: 'bik',
        },
        name: {
            title: 'ФИО плательщика',
            value: 'Иванов Джон Ли',
            name: 'name',
        },
        paymentPurpose: {
            title: 'Назначение платежа',
            value: `Оплата стоимости автомобиля по договору купли-продажи №БС${ DEAL_NUMBER }. НДС не облагается`,
            name: 'purpose',
        },
        paymentAmount: {
            title: 'Сумма платежа',
            value: '250 002 ₽',
            name: 'price',
        },
    };

    const paymentData = {
        bic: '044525974',
        account_number: '40702810910000899488',
        full_name: 'Мягков Олег Владимирович',
        inn: '7704340327',
        corr_account_number: '30101810145250000974',
        bank_name: 'АО "Тинькофф Банк"',
        receiver_name: 'ОБЩЕСТВО С ОГРАНИЧЕННОЙ ОТВЕТСТВЕННОСТЬЮ "ЯНДЕКС.ВЕРТИКАЛИ"',
        short_receiver_name: 'ООО "Яндекс.Вертикали"',
        totalPrice: '250002',
        paymentPurpose: `Оплата стоимости автомобиля по договору купли-продажи №БС${ DEAL_NUMBER }. НДС не облагается`,
        remainingPrice: 100000,
    };

    return shallow(
        <Provider store={ mockStore() }>
            <DealBankDetails
                details={ details }
                paymentData={ paymentData }
                sendMetrikaPageEvent={ jest.fn() }
                totalProvidedRub="0"
                isMobile={ false }
                isWarningClosed={ false }
                step={ BuyerStep.BUYER_PROVIDING_MONEY }
                onWarningClose={ jest.fn() }
            />
        </Provider>,
    ).dive();
}
