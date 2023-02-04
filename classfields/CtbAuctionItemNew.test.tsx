import React from 'react';
import { Provider } from 'react-redux';
import { shallow } from 'enzyme';

import type { Offer } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import Modal from 'auto-core/react/components/islands/Modal/Modal';
import salesMock from 'auto-core/react/dataDomain/sales/mocks';
import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.mock';

import type { C2bApplication } from 'auto-core/server/blocks/c2bAuction/types';
import { BuyoutFlow, C2bApplicationStatus } from 'auto-core/server/blocks/c2bAuction/types';

import CtbAuctionItemNew from './CtbAuctionItemNew';

const Context = createContextProvider(contextMock);
const initialState = {
    sales: salesMock.value(),
};
const store = mockStore(initialState);

describe('CtbAuctionItemNew - компонент для отрисовки заявки для c2b-аукциона в новом лк', () => {
    it('должен показывать активную заявку, если статус заявки активный', () => {
        const activeStatuses = [
            C2bApplicationStatus.NEW,
            C2bApplicationStatus.WAITING_INSPECTION,
            C2bApplicationStatus.INSPECTED,
            C2bApplicationStatus.AUCTION,
            C2bApplicationStatus.DEAL,
        ];

        const wrappers = activeStatuses.map(activeStatus => createShallowComponent({ status: activeStatus }));
        expect(wrappers.every(wrapper => wrapper.find('.CtbAuctionItemNew__statuses').length > 0)).toBe(true);
    });

    it('должен показывать успешно завершенную заявку, если статус заявки FINISHED', () => {
        const wrapper = createShallowComponent({ status: C2bApplicationStatus.FINISHED });

        const label = wrapper.find('.CtbAuctionItemNew__label');

        expect(label.exists()).toBe(true);
        expect(label.text()).toBe('Сделка состоялась');
    });

    it('должен показывать отклоненную заявку, если статус заявки REJECTED', () => {
        const wrapper = createShallowComponent({ status: C2bApplicationStatus.REJECTED });

        const label = wrapper.find('.CtbAuctionItemNew__label');

        expect(label.exists()).toBe(true);
        expect(label.text()).toBe('Сделка не состоялась');
    });

    it('должен показывать шаги со статусами', () => {
        const wrapper = createShallowComponent({ status: C2bApplicationStatus.NEW });

        expect(wrapper.find('.CtbAuctionItemNew__step')).toHaveLength(3);
    });

    it('должен показывать модалку при клике на ссылку', () => {
        const wrapper = createShallowComponent({ status: C2bApplicationStatus.DEAL });

        const exitIntentionLink = wrapper.find('.CtbAuctionItemNew__button');
        const modal = wrapper.find(Modal);

        expect(exitIntentionLink.exists()).toBe(true);
        expect(modal.exists()).toBe(true);
        expect(modal.prop('visible')).toBe(false);

        exitIntentionLink.simulate('click');

        const updatedModal = wrapper.find(Modal);

        expect(updatedModal.prop('visible')).toBe(true);
    });
});

function createApplicationMock(C2bApplicationStatus: C2bApplicationStatus) {
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
        buy_out_alg: BuyoutFlow.AUCTION,
    } as C2bApplication;
}

interface Args {
    status: C2bApplicationStatus;
}

function createShallowComponent({ status }: Args) {
    const application = createApplicationMock(status);

    return shallow(
        <Context>
            <Provider store={ store }>
                <CtbAuctionItemNew
                    application={ application }
                    dispatch={ jest.fn() }
                />
            </Provider>
        </Context>,
    ).dive().dive();
}
