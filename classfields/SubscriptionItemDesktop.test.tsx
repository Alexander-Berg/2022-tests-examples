jest.mock('auto-core/react/dataDomain/subscriptions/actions/remove');
jest.mock('auto-core/react/dataDomain/subscriptions/actions/sendEmailConfirmation');
jest.mock('auto-core/react/dataDomain/subscriptions/actions/updateEmailDelivery');

import React from 'react';
import { shallow } from 'enzyme';
import { Provider } from 'react-redux';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import removeSubscription from 'auto-core/react/dataDomain/subscriptions/actions/remove';
import sendEmailConfirmation from 'auto-core/react/dataDomain/subscriptions/actions/sendEmailConfirmation';
import updateEmailDelivery from 'auto-core/react/dataDomain/subscriptions/actions/updateEmailDelivery';
import userStateMock from 'auto-core/react/dataDomain/user/mocks';
import { subscriptionMock } from 'auto-core/react/dataDomain/subscriptions/mocks';

import SubscriptionItemDesktop from './SubscriptionItemDesktop';
import type { AppState, OwnProps } from './SubscriptionItemDesktop';

const removeSubscriptionMock = removeSubscription as jest.MockedFunction<typeof removeSubscription>;
removeSubscriptionMock.mockImplementation(() => () => Promise.resolve());

const sendEmailConfirmationMock = sendEmailConfirmation as jest.MockedFunction<typeof sendEmailConfirmation>;
sendEmailConfirmationMock.mockImplementation(() => () => Promise.resolve());

const updateEmailDeliveryMock = updateEmailDelivery as jest.MockedFunction<typeof updateEmailDelivery>;
updateEmailDeliveryMock.mockImplementation(() => () => Promise.resolve());

let props: OwnProps;
let initialState: AppState;

beforeEach(() => {
    props = {
        isFetching: false,
        subscription: subscriptionMock.value(),
    };
    initialState = {
        user: userStateMock.withAuth(true).value(),
    };
});

describe('выключение/выключение уведомлений', () => {
    it('передаст правильные параметры в экшен, если период был задан', () => {
        props.subscription = subscriptionMock.withEmailDelivery({ period: '604800s', enabled: false }).value();
        const page = shallowRenderComponent({ props, initialState });
        const checkbox = page.find('.SubscriptionItemDesktop__email-checkbox');
        checkbox.simulate('check', true);

        expect(updateEmailDeliveryMock).toHaveBeenCalledTimes(1);
        expect(updateEmailDeliveryMock).toHaveBeenCalledWith(props.subscription, { enabled: true, period: '604800s' });
    });

    it('передаст правильные параметры в экшен, если период не был задан', () => {
        props.subscription = subscriptionMock.withEmailDelivery({ period: '', enabled: false }).value();
        const page = shallowRenderComponent({ props, initialState });
        const checkbox = page.find('.SubscriptionItemDesktop__email-checkbox');
        checkbox.simulate('check', true);

        expect(updateEmailDeliveryMock).toHaveBeenCalledTimes(1);
        expect(updateEmailDeliveryMock).toHaveBeenCalledWith(props.subscription, { enabled: true, period: '14400s' });
    });

    it('отправит метрику', () => {
        const page = shallowRenderComponent({ props, initialState });
        const checkbox = page.find('.SubscriptionItemDesktop__email-checkbox');
        checkbox.simulate('check', false);

        expect(contextMock.metrika.sendPageAuthEvent).toHaveBeenCalledTimes(1);
        expect(contextMock.metrika.sendPageAuthEvent).toHaveBeenCalledWith([ 'saved-search', 'unsubscribed' ]);
    });
});

describe('при смене периода уведомлений', () => {
    it('если новый период не 0', () => {
        const page = shallowRenderComponent({ props, initialState });
        const select = page.find('.SubscriptionItemDesktop__period-select');
        select.simulate('change', [ '604800s' ]);

        expect(updateEmailDeliveryMock).toHaveBeenCalledTimes(1);
        expect(updateEmailDeliveryMock).toHaveBeenCalledWith(props.subscription, { enabled: true, period: '604800s' });
    });

    it('если новый период 0', () => {
        const page = shallowRenderComponent({ props, initialState });
        const select = page.find('.SubscriptionItemDesktop__period-select');
        select.simulate('change', [ '0s' ]);

        expect(updateEmailDeliveryMock).toHaveBeenCalledTimes(1);
        expect(updateEmailDeliveryMock).toHaveBeenCalledWith(props.subscription, { enabled: false, period: '0s' });
    });

    it('отправит метрику', () => {
        const page = shallowRenderComponent({ props, initialState });
        const select = page.find('.SubscriptionItemDesktop__period-select');
        select.simulate('change', [ '604800s' ]);

        expect(contextMock.metrika.sendPageAuthEvent).toHaveBeenCalledTimes(1);
        expect(contextMock.metrika.sendPageAuthEvent).toHaveBeenCalledWith([ 'saved-search', 'subscribe-change', '604800s' ]);
    });
});

describe('привязка адреса почты', () => {
    beforeEach(() => {
        initialState.user = userStateMock.withAuth(false).value();
    });

    it('покажет ошибку если поле не заполнено', () => {
        const page = shallowRenderComponent({ props, initialState });
        const form = page.find('form');
        form.simulate('submit', { preventDefault: () => {} });

        const input = page.find('.SubscriptionItemDesktop__email-input');
        expect(input.prop('error')).toBe('Укажите вашу электронную почту');
    });

    it('покажет ошибку если поле заполнено неправильно', () => {
        const page = shallowRenderComponent({ props, initialState });
        const input = page.find('.SubscriptionItemDesktop__email-input');
        input.simulate('change', 'foo.bar');
        const form = page.find('form');
        form.simulate('submit', { preventDefault: () => {} });

        const updatedInput = page.find('.SubscriptionItemDesktop__email-input');
        expect(updatedInput.prop('error')).toBe('Электронная почта указана неверно');
    });

    it('вызовет экшен если всё ок', () => {
        const page = shallowRenderComponent({ props, initialState });
        const input = page.find('.SubscriptionItemDesktop__email-input');
        input.simulate('change', 'foo@bar.ru');
        const form = page.find('form');
        form.simulate('submit', { preventDefault: () => { } });

        expect(sendEmailConfirmationMock).toHaveBeenCalledTimes(1);
        expect(sendEmailConfirmationMock).toHaveBeenCalledWith({
            id: props.subscription.id,
            email: 'foo@bar.ru',
        });
    });

    it('отправит метрику если всё ок', () => {
        const page = shallowRenderComponent({ props, initialState });
        const input = page.find('.SubscriptionItemDesktop__email-input');
        input.simulate('change', 'foo@bar.ru');
        const form = page.find('form');
        form.simulate('submit', { preventDefault: () => { } });

        expect(contextMock.metrika.sendPageAuthEvent).toHaveBeenCalledTimes(1);
        expect(contextMock.metrika.sendPageAuthEvent).toHaveBeenCalledWith([ 'saved-search', 'subscribe' ]);
    });
});

it('в поиске по дилеру если нет других параметров добавит строку про официальность', () => {
    props.subscription = subscriptionMock
        .withParams({ dealer_id: '123456' })
        .withParamsDescription({ paramsInfo: [] })
        .withSalonViews({ code: 'dealer', is_official: true, loyalty_program: false })
        .value();
    const page = shallowRenderComponent({ props, initialState });
    const description = page.find('.SubscriptionItemDesktop__description');

    expect(description.text()).toBe('Официальный дилер');
});

function shallowRenderComponent({ initialState, props }: { props: OwnProps; initialState: AppState }) {
    const ContextProvider = createContextProvider(contextMock);
    const store = mockStore(initialState);

    const page = shallow(
        <ContextProvider>
            <Provider store={ store }>
                <SubscriptionItemDesktop { ...props }/>
            </Provider>
        </ContextProvider>,
    );

    return page.dive().dive().dive();
}
