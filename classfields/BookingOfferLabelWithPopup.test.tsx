import 'jest-enzyme';
import mockdate from 'mockdate';
import React from 'react';
import { Provider } from 'react-redux';
import { shallow } from 'enzyme';

import { OfferStatus } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';

import type { ThunkMockStore } from 'autoru-frontend/mocks/mockStore';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';
import mockStore from 'autoru-frontend/mocks/mockStore';
import { getBunkerMock } from 'autoru-frontend/mockData/state/bunker.mock';

import { DAY } from 'auto-core/lib/consts';

import STATUSES from 'auto-core/react/dataDomain/booking/dicts/booking_interface_statuses';
import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';

import type { TAppState } from './BookingOfferLabelWithPopup';
import BookingOfferLabelWithPopup from './BookingOfferLabelWithPopup';

let store: ThunkMockStore<TAppState>;
beforeEach(() => {
    mockdate.set('2020-12-01');
    store = mockStore({
        bunker: getBunkerMock([ 'common/booking' ]),
    });
});

afterEach(() => {
    mockdate.reset();
});

describe('статус оффера', () => {
    it('если оффер активен и забронирован, рисует лейбл', () => {
        const offer = cloneOfferWithHelpers(offerMock)
            .withStatus(OfferStatus.ACTIVE)
            .withIsOwner(false)
            .withBooking()
            .value();
        const tree = shallow(
            <Provider store={ store }>
                <BookingOfferLabelWithPopup offer={ offer }/>
            </Provider>,
        ).dive().dive();

        expect(tree).not.toBeEmptyRender();
    });

    it('если оффер продан, не рисует лейбл о возможности забронировать это авто', () => {
        const offer = cloneOfferWithHelpers(offerMock)
            .withStatus(OfferStatus.INACTIVE)
            .withIsOwner(false)
            .withBooking()
            .value();
        const tree = shallow(
            <Provider store={ store }>
                <BookingOfferLabelWithPopup offer={ offer }/>
            </Provider>,
        ).dive().dive();

        expect(tree).toBeEmptyRender();
    });
});

describe('тексты', () => {
    const FIRST_BOOKING_DAY = 1597659169559;
    const LAST_BOOKING_DAY = FIRST_BOOKING_DAY + 5 * DAY;
    const offer = cloneOfferWithHelpers(offerMock)
        .withStatus(OfferStatus.ACTIVE)
        .withIsOwner(false)
        .withBooking()
        .withBookingStatus(STATUSES.BOOKED, {
            period: {
                from: new Date(FIRST_BOOKING_DAY).toISOString(),
                to: new Date(LAST_BOOKING_DAY).toISOString(),
            },
        })
        .value();

    it('корректно заменяет маркер в текстах из бункера на дедлайн', () => {
        const tree = shallow(
            <Provider store={ store }>
                <BookingOfferLabelWithPopup offer={ offer }/>
            </Provider>,
        ).dive().dive();

        expect(tree.find('InfoPopup').props().content).toMatchSnapshot();
    });

    it('не падает если в текстах из бункера ошибка', () => {
        const tree = shallow(
            <Provider store={ mockStore({ bunker: { 'common/booking': {} } }) }>
                <BookingOfferLabelWithPopup offer={ offer }/>
            </Provider>,
        ).dive().dive();

        expect(tree.find('InfoPopup').props().content).toBeNull();
    });
});
