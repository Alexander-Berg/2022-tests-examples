/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/actions/scroll', () => jest.fn());
jest.mock('auto-core/lib/event-log/statApi', () => ({ logImmediately: jest.fn() }));
jest.mock('auto-core/react/dataDomain/state/actions/authModalClose', () => jest.fn());
jest.mock('auto-core/react/dataDomain/state/actions/authModalOpen', () => jest.fn());

import _ from 'lodash';
import React from 'react';
import { shallow } from 'enzyme';

import { Status } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_resolution_enums';

// Mocks
import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import configStateMock from 'auto-core/react/dataDomain/config/mock';
import cardVinReportFree from 'auto-core/react/dataDomain/vinReport/mocks/freeReport-mercedes.mock';
import offer from 'auto-core/react/dataDomain/card/mocks/card.cars.mock';
import userWithAuthMock from 'auto-core/react/dataDomain/user/mocks/withAuth.mock';
import userWithoutAuthMock from 'auto-core/react/dataDomain/user/mocks/withoutAuth.mock';

import type { OwnProps } from './ModalVinReportButtons';
import ModalVinReportButtons from './ModalVinReportButtons';

const REQUIRED_PROPS: OwnProps = {
    offer: cloneOfferWithHelpers(offer).value(),
};

const state = {
    bunker: {},
    card: offer,
    config: configStateMock.withPageType('card').value(),
    state: {},
    user: userWithAuthMock,
    vinReport: { data: cardVinReportFree },
};

const createStore = (customState = {}) => mockStore({ ...state, ...customState });

let context: typeof contextMock;
let store: any;

beforeEach(() => {
    context = _.cloneDeep(contextMock);
    store = createStore();
});

const renderComponent = (props: OwnProps, customStore?: any) => {
    return shallow(
        <ModalVinReportButtons { ...props }/>,
        { context: { ...context, store: customStore || store } },
    ).dive();
};

describe('не показываем ничего', () => {
    const renderComponent = (customStore = store, customProps?: OwnProps) => {
        return shallow(
            <ModalVinReportButtons { ...(customProps || REQUIRED_PROPS) }/>,
            { context: { ...context, store: customStore } },
        ).dive();
    };

    it('если отчёта нет у оффера отчет в статусе, который мы не рисуем', () => {
        const customStore = createStore({ vinReport: {} });
        const customProps = { offer: cloneOfferWithHelpers(offer).withVinResolution(Status.INVALID).value() };

        expect(renderComponent(customStore, customProps)).toBeEmptyRender();
    });
});

describe('метрики', () => {
    describe('на показ', () => {
        it('незалогин', () => {
            renderComponent({ offer: cloneOfferWithHelpers(offer).withIsOwner(false).value() }, createStore({ user: userWithoutAuthMock }));

            expect(context.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
            expect(context.metrika.sendPageEvent.mock.calls[0][0]).toStrictEqual([ 'exp_history_report_mini', 'show_go_to_standalone', 'no_login' ]);
        });

        it('не владелец', () => {
            renderComponent({ offer: cloneOfferWithHelpers(offer).withIsOwner(false).value() });

            expect(context.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
            expect(context.metrika.sendPageEvent.mock.calls[0][0]).toStrictEqual([ 'exp_history_report_mini', 'show_go_to_standalone', 'not_owner' ]);
        });

        it('владелец', () => {
            renderComponent({ offer: cloneOfferWithHelpers(offer).withIsOwner(true).value() });

            expect(context.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
            expect(context.metrika.sendPageEvent.mock.calls[0][0]).toStrictEqual([ 'exp_history_report_mini', 'show_go_to_standalone', 'owner' ]);
        });
    });

    describe('на клик', () => {
        const renderWrapper = (props: OwnProps, store?: any) => {
            const wrapper = renderComponent(props, store);
            wrapper.find('Button').simulate('click');
        };

        it('незалогин', () => {
            renderWrapper({ offer: cloneOfferWithHelpers(offer).withIsOwner(false).value() }, createStore({ user: userWithoutAuthMock }));

            expect(context.metrika.sendPageEvent).toHaveBeenCalledTimes(2);
            expect(context.metrika.sendPageEvent.mock.calls[1][0]).toStrictEqual([ 'exp_history_report_mini', 'click_go_to_standalone', 'no_login' ]);
        });

        it('не владелец', () => {
            renderWrapper({ offer: cloneOfferWithHelpers(offer).withIsOwner(false).value() });

            expect(context.metrika.sendPageEvent).toHaveBeenCalledTimes(2);
            expect(context.metrika.sendPageEvent.mock.calls[1][0]).toStrictEqual([ 'exp_history_report_mini', 'click_go_to_standalone', 'not_owner' ]);
        });

        it('владелец', () => {
            renderWrapper({ offer: cloneOfferWithHelpers(offer).withIsOwner(true).value() });

            expect(context.metrika.sendPageEvent).toHaveBeenCalledTimes(2);
            expect(context.metrika.sendPageEvent.mock.calls[1][0]).toStrictEqual([ 'exp_history_report_mini', 'click_go_to_standalone', 'owner' ]);
        });
    });
});
