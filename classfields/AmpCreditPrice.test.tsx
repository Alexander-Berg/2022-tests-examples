jest.mock('auto-core/react/actions/scroll');
jest.mock('auto-core/react/dataDomain/credit/actions/openCreditApplicationFormModal', () => {
    return jest.fn(() => ({ type: 'mock_openCreditApplicationFormModal' }));
});

declare var global: { location: Record<string, any> };
const { location } = global;

beforeEach(() => {
    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
    // @ts-ignore
    delete global.location;
    global.location = {
        assign: jest.fn(),
    };
});

afterEach(() => {
    global.location = location;
});

import 'jest-enzyme';
import React from 'react';
import { shallow } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';
import STATUSES from 'auto-core/react/dataDomain/booking/dicts/booking_interface_statuses';

import type { Offer } from 'auto-core/types/proto/auto/api/api_offer_model';

import AmpCreditPrice from './AmpCreditPrice';

describe('цена оффера в кредит', () => {
    it('не рендерится, если нет информации о платеже', () => {
        const offer = cloneOfferWithHelpers(offerMock)
            .withIsOwner(false)
            .value();

        const tree = shallowRenderCreditPrice(offer);

        expect(tree).toBeEmptyRender();
    });

    it('не рендерится для владельца', () => {
        const offer = cloneOfferWithHelpers(offerMock)
            .withIsOwner(true)
            .value();
        const tree = shallowRenderCreditPrice(offer);

        expect(tree).toBeEmptyRender();
    });

    it('не рендерится если оффер забронирован', () => {
        const offer = cloneOfferWithHelpers(offerMock)
            .withIsOwner(false)
            .withBookingStatus(STATUSES.BOOKED)
            .value();
        const tree = shallowRenderCreditPrice(offer);

        expect(tree).toBeEmptyRender();
    });

    it('рендерится, если есть информация о платеже', () => {
        const offer = cloneOfferWithHelpers(offerMock)
            .withCreditPrecondition({ monthly_payment: '27500' })
            .withIsOwner(false)
            .value();

        const tree = shallowRenderCreditPrice(offer);

        expect(tree).not.toBeEmptyRender();
    });
});

function shallowRenderCreditPrice(offer: Offer) {
    const Context = createContextProvider(contextMock);

    return shallow(
        <Context>
            <AmpCreditPrice offer={ offer }/>
        </Context>,
    ).dive();
}
