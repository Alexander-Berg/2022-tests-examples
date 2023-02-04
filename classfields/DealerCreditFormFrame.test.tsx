/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import React from 'react';
import { shallow } from 'enzyme';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import offer from 'auto-core/react/dataDomain/card/mocks/card.cars.mock';

import DealerCreditFormFrame from './DealerCreditFormFrame';

const ContextProvider = createContextProvider(contextMock);

const offerMock = cloneOfferWithHelpers(offer).withSalon().withDealerCredit().value();

const defaultProps = {
    offer: offerMock,
    dealersWithMultipleLocations: [ '1' ],
    creditOptions: {
        monthlyPayment: 10000,
        fee: 0.12,
        period: 12,
        name: 'Имя Фамилия',
        phone: '+79161234567',
    },
};

describe('должен правильно сформировать ссылку для iframe еКредита', () => {
    const urlCommonPart = 'https://credit-online.e-credit.one?token=yaonline&product_type=0';

    it('если передали имя и фамилию', () => {
        const tree = shallowRenderComponent();
        expect(tree.find('iframe').prop('src')).toEqual(
            urlCommonPart +
            '&car_condition=2&brand=Ford&model=EcoSport&price=855000' +
            '&client_phone=9161234567&initial_fee_money=0.12&monthly_payment=10000&period=144&rate=8' +
            '&dealer_id=77777&client_name=%D0%98%D0%BC%D1%8F&client_surname=%D0%A4%D0%B0%D0%BC%D0%B8%D0%BB%D0%B8%D1%8F',
        );
    });

    it('если передали имя, фамилию и отчество', () => {
        const creditOptions = { ...defaultProps.creditOptions, name: 'Фамилия Имя Отчество' };
        const tree = shallowRenderComponent({ ...defaultProps, creditOptions });
        expect(tree.find('iframe').prop('src')).toEqual(
            urlCommonPart +
            '&car_condition=2&brand=Ford&model=EcoSport&price=855000' +
            '&client_phone=9161234567&initial_fee_money=0.12&monthly_payment=10000&period=144&rate=8' +
            '&dealer_id=77777&client_surname=%D0%A4%D0%B0%D0%BC%D0%B8%D0%BB%D0%B8%D1%8F&client_name=%D0%98%D0%BC%D1%8F' +
            '&client_middle_name=%D0%9E%D1%82%D1%87%D0%B5%D1%81%D1%82%D0%B2%D0%BE');
    });

    it('если дилер входит в список дилеров с несколькими локациями', () => {
        const dealersWithMultipleLocations = [ offerMock.salon!.client_id ];
        const tree = shallowRenderComponent({ ...defaultProps, dealersWithMultipleLocations });
        expect(tree.find('iframe').prop('src')).toEqual(
            urlCommonPart +
            '&car_condition=2&brand=Ford&model=EcoSport&price=855000' +
            '&client_phone=9161234567&initial_fee_money=0.12&monthly_payment=10000&period=144&rate=8' +
            '&id_type=2&dealer_id=55.757668-37.843274&client_name=%D0%98%D0%BC%D1%8F&client_surname=%D0%A4%D0%B0%D0%BC%D0%B8%D0%BB%D0%B8%D1%8F');
    });
});

function shallowRenderComponent(props = defaultProps) {
    return shallow(
        <ContextProvider>
            <DealerCreditFormFrame { ...props }/>
        </ContextProvider>,
    ).dive();
}
