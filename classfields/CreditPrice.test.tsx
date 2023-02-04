/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

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
import { Provider } from 'react-redux';
import type { MockStoreEnhanced } from 'redux-mock-store';

import mockStore from 'autoru-frontend/mocks/mockStore';
import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';
import configMock from 'auto-core/react/dataDomain/config/mock';
import creditApplicationMock from 'auto-core/react/dataDomain/credit/mocks/creditApplication.mockchain';
import openCreditApplicationFormModal from 'auto-core/react/dataDomain/credit/actions/openCreditApplicationFormModal';
import scroll from 'auto-core/react/actions/scroll';
import STATUSES from 'auto-core/react/dataDomain/booking/dicts/booking_interface_statuses';

import type { Offer } from 'auto-core/types/proto/auto/api/api_offer_model';

import CreditPrice from './CreditPrice';

const creditApplicationWithDraft = creditApplicationMock().value();
const emptyStore = mockStore({
    user: { data: {} },
});

describe('цена оффера в кредит', () => {
    it('не рендерится, если нет информации о платеже', () => {
        const offer = cloneOfferWithHelpers(offerMock)
            .withIsOwner(false)
            .value();

        const tree = shallowRenderCreditPrice({ offer, store: emptyStore });

        expect(tree).toBeEmptyRender();
    });

    it('не рендерится для владельца', () => {
        const offer = cloneOfferWithHelpers(offerMock)
            .withIsOwner(true)
            .withCreditPrecondition()
            .value();
        const tree = shallowRenderCreditPrice({ offer, store: emptyStore });

        expect(tree).toBeEmptyRender();
    });

    it('не рендерится если оффер забронирован', () => {
        const offer = cloneOfferWithHelpers(offerMock)
            .withIsOwner(false)
            .withBookingStatus(STATUSES.BOOKED)
            .withCreditPrecondition()
            .value();
        const tree = shallowRenderCreditPrice({ offer, store: emptyStore });

        expect(tree).toBeEmptyRender();
    });

    it('не рендерится если тачка новая и без легаси дилерского кредита', () => {
        const offer = cloneOfferWithHelpers(offerMock)
            .withIsOwner(false)
            .withSection('new')
            .withCreditPrecondition()
            .value();
        const tree = shallowRenderCreditPrice({ offer, store: emptyStore });

        expect(tree).toBeEmptyRender();
    });

    it('рендерится, если есть информация о платеже', () => {
        const offer = cloneOfferWithHelpers(offerMock)
            .withCreditPrecondition({ monthly_payment: '27500' })
            .withIsOwner(false)
            .value();

        const tree = shallowRenderCreditPrice({ offer, store: emptyStore });

        expect(tree).not.toBeEmptyRender();
    });

    it('если есть заявка, откроет карточку авто', () => {
        const context = {
            ...contextMock,
            link: () => 'card_link_mock',
        };
        const offer = cloneOfferWithHelpers(offerMock)
            .withCreditPrecondition({ monthly_payment: '27500' })
            .withIsOwner(false)
            .value();
        const store = mockStore({
            credit: { application: { data: { credit_application: creditApplicationWithDraft } } },
            user: { data: {} },
        });
        const tree = shallowRenderCreditPrice({ offer, store, context });

        tree.find('.CreditPrice').simulate('click');
        expect(window.location.href).toBe('card_link_mock#credit-card');
    });

    it('подскроллит к блоку кредитов, если уже на карточке авто', () => {
        const offer = cloneOfferWithHelpers(offerMock)
            .withCreditPrecondition({ monthly_payment: '27500' })
            .withIsOwner(false)
            .value();
        const store = mockStore({
            config: configMock
                .withPageType('card')
                .withPageParams({ sale_id: '1085562758' })
                .value(),
            user: { data: {} },
        });
        const tree = shallowRenderCreditPrice({ offer, store });

        tree.find('.CreditPrice').simulate('click');
        expect(scroll).toHaveBeenCalledTimes(1);
        expect(scroll).toHaveBeenCalledWith('credit-card', { duration: 600, offset: -52 });
    });

    it('если нет активной заявки, откроет модальное окно с короткой заявкой', () => {
        const offer = cloneOfferWithHelpers(offerMock)
            .withCreditPrecondition({ monthly_payment: '27500' })
            .withIsOwner(false)
            .value();

        const tree = shallowRenderCreditPrice({ offer, store: emptyStore });

        tree.find('.CreditPrice').simulate('click');
        expect(openCreditApplicationFormModal).toHaveBeenCalledTimes(1);
    });

    it('для е-кредита', () => {
        const offer = cloneOfferWithHelpers(offerMock)
            .withCreditPrecondition({ monthly_payment: '27500' })
            .withIsOwner(false)
            .withEcreditPrecondition()
            .value();

        const tree = shallowRenderCreditPrice({ offer, store: emptyStore });

        expect(tree).not.toBeEmptyRender();
    });

    it('для легаси дилерских кредитов', () => {
        const offer = cloneOfferWithHelpers(offerMock)
            .withCreditPrecondition({ monthly_payment: '27500' })
            .withIsOwner(false)
            .value();

        const tree = shallowRenderCreditPrice({ offer, store: emptyStore });

        expect(tree).not.toBeEmptyRender();
    });

    it('для легаси дилерских кредитов (новое авто)', () => {
        const offer = cloneOfferWithHelpers(offerMock)
            .withCreditPrecondition({ monthly_payment: '27500' })
            .withDealerCredit()
            .withSection('new')
            .withIsOwner(false)
            .value();

        const tree = shallowRenderCreditPrice({ offer, store: emptyStore });

        expect(tree).not.toBeEmptyRender();
    });
});

interface RenderParams {
    store: MockStoreEnhanced;
    offer: Offer;
    context?: any;
}

function shallowRenderCreditPrice(params: RenderParams) {
    const Context = createContextProvider(params.context || contextMock);

    return shallow(
        <Context>
            <Provider store={ params.store }>
                <CreditPrice offer={ params.offer }/>
            </Provider>
        </Context>,
    ).dive().dive().dive().dive();
}
