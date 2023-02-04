jest.mock('auto-core/react/dataDomain/subscriptions/actions/sendEmailConfirmation');
jest.mock('auto-core/react/dataDomain/subscriptions/actions/updateEmailDelivery');

import React from 'react';
import type { ShallowWrapper } from 'enzyme';
import { shallow } from 'enzyme';
import { Provider } from 'react-redux';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import sendEmailConfirmation from 'auto-core/react/dataDomain/subscriptions/actions/sendEmailConfirmation';
import updateEmailDelivery from 'auto-core/react/dataDomain/subscriptions/actions/updateEmailDelivery';
import userStateMock from 'auto-core/react/dataDomain/user/mocks';
import { subscriptionStateMock } from 'auto-core/react/dataDomain/subscriptions/mocks';
import configStateMock from 'auto-core/react/dataDomain/config/mock';

import type { MobileAppState } from 'www-mobile/react/MobileAppState';

import PageSearches from './PageSearches';

const sendEmailConfirmationMock = sendEmailConfirmation as jest.MockedFunction<typeof sendEmailConfirmation>;
sendEmailConfirmationMock.mockImplementation(() => () => Promise.resolve());

const updateEmailDeliveryMock = updateEmailDelivery as jest.MockedFunction<typeof updateEmailDelivery>;
updateEmailDeliveryMock.mockImplementation(() => () => Promise.resolve());

let initialState: Partial<MobileAppState>;
beforeEach(() => {
    initialState = {
        config: configStateMock.value(),
        subscriptions: subscriptionStateMock.value(),
        user: userStateMock.withAuth(true).value(),
    };
});

describe('при клике на статус подписки', () => {
    it('если у пользователя есть email откроет модал с выбором периода', () => {
        const page = shallowRenderComponent({ initialState });
        const item = page.find('Connect(SubscriptionItemMobile)').at(0);
        item.simulate('changePeriodClick', initialState.subscriptions?.data[0]?.data.id);

        const periodModal = page.find('SubscriptionModalDeliveryPeriod');
        expect(periodModal.prop('isOpened')).toBe(true);
        expect(periodModal.prop('value')).toBe('14400s');

        const emailModal = page.find('SubscriptionModalEmail');
        expect(emailModal.prop('visible')).toBe(false);
    });

    it('если у пользователя нет email откроет модал для ввода email', () => {
        initialState.user = userStateMock.withAuth(true).withEmails([]).value();
        const page = shallowRenderComponent({ initialState });
        const item = page.find('Connect(SubscriptionItemMobile)').at(0);
        item.simulate('changePeriodClick', initialState.subscriptions?.data[0]?.data.id);

        const periodModal = page.find('SubscriptionModalDeliveryPeriod');
        expect(periodModal.prop('isOpened')).toBe(false);

        const emailModal = page.find('SubscriptionModalEmail');
        expect(emailModal.prop('visible')).toBe(true);
        expect(emailModal.prop('subscription')).toBe(initialState.subscriptions?.data[0]?.data);
    });
});

describe('при смене периода подписки', () => {
    let page: ShallowWrapper;

    beforeEach(() => {
        page = shallowRenderComponent({ initialState });
        const item = page.find('Connect(SubscriptionItemMobile)').at(0);
        item.simulate('changePeriodClick', initialState.subscriptions?.data[0]?.data.id);

        const periodModal = page.find('SubscriptionModalDeliveryPeriod');
        periodModal.simulate('change', '604800s');
    });

    it('вызовет экшен', () => {
        expect(updateEmailDeliveryMock).toHaveBeenCalledTimes(1);
        expect(updateEmailDeliveryMock).toHaveBeenCalledWith(initialState.subscriptions?.data[0]?.data, {
            enabled: true,
            period: '604800s',
        });
    });

    it('отправит метрику', () => {
        expect(contextMock.metrika.sendPageAuthEvent).toHaveBeenCalledTimes(1);
        expect(contextMock.metrika.sendPageAuthEvent).toHaveBeenCalledWith([ 'saved-search', 'subscribe-change', '604800s' ]);
    });

    it('закроет модал', () => {
        const periodModal = page.find('SubscriptionModalDeliveryPeriod');
        expect(periodModal.prop('isOpened')).toBe(false);
    });
});

function shallowRenderComponent({ initialState }: { initialState: Partial<MobileAppState> }) {
    const ContextProvider = createContextProvider(contextMock);
    const store = mockStore(initialState);

    const page = shallow(
        <ContextProvider>
            <Provider store={ store }>
                <PageSearches/>
            </Provider>
        </ContextProvider>,
    );

    return page.dive().dive().dive();
}
