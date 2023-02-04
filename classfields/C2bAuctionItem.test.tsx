jest.mock('react-redux', () => ({
    ...jest.requireActual('react-redux'),
    useSelector: () => undefined,
}));

import React from 'react';
import { Provider } from 'react-redux';
import { mount } from 'enzyme';

import type { Offer } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import salesMock from 'auto-core/react/dataDomain/sales/mocks';
import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.mock';
import { SUPPORT_PHONE_NUMBER } from 'auto-core/react/components/common/C2BAuctions/constants/supportPhoneNumber';

import { C2bApplicationStatus, BuyoutFlow } from 'auto-core/server/blocks/c2bAuction/types';

import type TContext from 'auto-core/types/TContext';

import C2bAuctionItem from './C2bAuctionItem';

const Context = createContextProvider(contextMock);
const initialState = {
    sales: salesMock.value(),
};
const store = mockStore(initialState);

describe('C2bAuctionItem - компонент для отрисовки заявки для c2b-аукциона', () => {
    it('Рендерит экран оценки', () => {
        const statuses = [
            { status: C2bApplicationStatus.UNSET, alg: BuyoutFlow.WITH_PRE_OFFERS },
            { status: C2bApplicationStatus.NEW, alg: BuyoutFlow.WITH_PRE_OFFERS },
            { status: C2bApplicationStatus.WAITING_DOCUMENTS, alg: BuyoutFlow.WITH_PRE_OFFERS },
            { status: C2bApplicationStatus.PRE_OFFERS, alg: BuyoutFlow.WITH_PRE_OFFERS },
        ];

        const wrappers = statuses.map(({ alg, status }) => renderComponent({ status, alg }));
        expect(wrappers.every((wrapper) => wrapper.find('.C2bAuctionItemActive__stepTitle').text() === 'Оценка')).toBe(true);
    });

    it('Рендерит экран готовой ценки', () => {
        const statuses = [
            { status: C2bApplicationStatus.CONFIRM_PRE_OFFERS, alg: BuyoutFlow.WITH_PRE_OFFERS },
            { status: C2bApplicationStatus.CONFIRM_PRE_OFFERS, alg: BuyoutFlow.AUCTION },
        ];

        const wrappers = statuses.map(({ alg, status }) => renderComponent({ status, alg }));
        expect(wrappers.every((wrapper) => wrapper.find('.C2bAuctionItemActive__stepTitle').text() === 'Оценка готова')).toBe(true);
    });

    it('Рендерит экран сбора предложений', () => {
        const statuses = [
            { status: C2bApplicationStatus.FINAL_OFFERS, alg: BuyoutFlow.WITH_PRE_OFFERS },
            { status: C2bApplicationStatus.AUCTION, alg: BuyoutFlow.WITH_PRE_OFFERS },
            { status: C2bApplicationStatus.INSPECTED, alg: BuyoutFlow.WITH_PRE_OFFERS },
            { status: C2bApplicationStatus.FINAL_OFFERS, alg: BuyoutFlow.AUCTION },
            { status: C2bApplicationStatus.AUCTION, alg: BuyoutFlow.AUCTION },
            { status: C2bApplicationStatus.INSPECTED, alg: BuyoutFlow.AUCTION },
        ];

        const wrappers = statuses.map(({ alg, status }) => renderComponent({ status, alg }));
        expect(wrappers.every((wrapper) => wrapper.find('.C2bAuctionItemActive__stepTitle').text() === 'Сбор предложений')).toBe(true);
    });

    it('Рендерит экран итоговой цены', () => {
        const statuses = [
            { status: C2bApplicationStatus.DEAL, alg: BuyoutFlow.WITH_PRE_OFFERS },
            { status: C2bApplicationStatus.DEAL, alg: BuyoutFlow.AUCTION },
        ];

        const wrappers = statuses.map(({ alg, status }) => renderComponent({ status, alg }));
        expect(wrappers.every((wrapper) => wrapper.find('.C2bAuctionItemActive__stepTitle').text() === 'Итоговая цена')).toBe(true);
    });

    it('Рендерит экран осмотра', () => {
        const statuses = [
            { status: C2bApplicationStatus.UNSET, alg: BuyoutFlow.AUCTION },
            { status: C2bApplicationStatus.NEW, alg: BuyoutFlow.AUCTION },
            { status: C2bApplicationStatus.WAITING_DOCUMENTS, alg: BuyoutFlow.AUCTION },
            { status: C2bApplicationStatus.PRE_OFFERS, alg: BuyoutFlow.AUCTION },
            { status: C2bApplicationStatus.WAITING_INSPECTION, alg: BuyoutFlow.AUCTION },
            { status: C2bApplicationStatus.WAITING_INSPECTION, alg: BuyoutFlow.WITH_PRE_OFFERS },
        ];

        const wrappers = statuses.map(({ alg, status }) => renderComponent({ status, alg }));
        expect(wrappers.every((wrapper) => wrapper.find('.C2bAuctionItemActive__stepTitle').text() === 'Осмотр')).toBe(true);
    });

    it('Рендерит экран состоявшейся сделки', () => {
        const statuses = [
            { status: C2bApplicationStatus.FINISHED, alg: BuyoutFlow.AUCTION },
            { status: C2bApplicationStatus.FINISHED, alg: BuyoutFlow.WITH_PRE_OFFERS },
        ];

        const wrappers = statuses.map(({ alg, status }) => renderComponent({ status, alg }));
        expect(wrappers.every((wrapper) => wrapper.find('.C2bAuctionItemFinished__description').text() === 'Сделка\nсостоялась')).toBe(true);
    });

    it('Рендерит экран несостоявшейся сделки', () => {
        const statuses = [
            { status: C2bApplicationStatus.REJECTED, alg: BuyoutFlow.AUCTION },
            { status: C2bApplicationStatus.REJECTED, alg: BuyoutFlow.WITH_PRE_OFFERS },
        ];

        const wrappers = statuses.map(({ alg, status }) => renderComponent({ status, alg }));
        expect(wrappers.every((wrapper) => wrapper.find('.C2bAuctionItemFinished__description').text() === 'Сделка\nне состоялась')).toBe(true);
    });

    it('Рендерит номер телефона', () => {
        const statuses = [
            { status: C2bApplicationStatus.FINAL_OFFERS, alg: BuyoutFlow.WITH_PRE_OFFERS },
            { status: C2bApplicationStatus.AUCTION, alg: BuyoutFlow.WITH_PRE_OFFERS },
            { status: C2bApplicationStatus.INSPECTED, alg: BuyoutFlow.WITH_PRE_OFFERS },
            { status: C2bApplicationStatus.DEAL, alg: BuyoutFlow.WITH_PRE_OFFERS },
            { status: C2bApplicationStatus.FINAL_OFFERS, alg: BuyoutFlow.AUCTION },
            { status: C2bApplicationStatus.AUCTION, alg: BuyoutFlow.AUCTION },
            { status: C2bApplicationStatus.INSPECTED, alg: BuyoutFlow.AUCTION },
            { status: C2bApplicationStatus.DEAL, alg: BuyoutFlow.WITH_PRE_OFFERS },
        ];

        const wrappers = statuses.map(({ alg, status }) => renderComponent({ status, alg }));

        expect(wrappers.every((wrapper) => wrapper.find(`a[href="${ SUPPORT_PHONE_NUMBER.telLink }"]`).exists())).toBe(true);
    });
});

function createApplicationMock(C2bApplicationStatus: C2bApplicationStatus, alg: BuyoutFlow) {
    const carOfferMock = {
        ...offerMock,
        car_info: offerMock.vehicle_info,
    } as unknown as Offer;

    return {
        offer: carOfferMock,
        offer_id: '123',
        id: '1',
        inspect_dates: [ '11-10-1991' ],
        inspect_place: {
            lat: 30.01,
            lon: 19.69,
            address: 'Москва, Мавзолей',
        },
        inspect_time: 'с 12 утра',
        price_prediction: {
            from: 1,
            to: 100000000,
        },
        status: C2bApplicationStatus,
        user_id: '1233',
        buy_out_alg: alg,
    };
}

interface Args {
    status: C2bApplicationStatus;
    alg: BuyoutFlow;
}

function renderComponent({ alg, status }: Args) {
    const application = createApplicationMock(status, alg);

    return mount(
        <Context>
            <Provider store={ store }>
                <C2bAuctionItem
                    application={ application }
                    dispatch={ jest.fn() }
                    context={ contextMock as unknown as TContext }
                />
            </Provider>
        </Context>,
    );
}
