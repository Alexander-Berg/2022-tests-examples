jest.mock('auto-core/react/dataDomain/dealerCallback/actions/submit', () => {
    return jest.fn().mockReturnValue({ type: 'submitCallback_action' });
});
jest.mock('auto-core/react/dataDomain/tradein/actions/submitForm');
jest.mock('auto-core/react/dataDomain/dealerCallback/helpers/sendFrontLogAfterCallback', () => jest.fn());

import _ from 'lodash';
import { Provider } from 'react-redux';
import React from 'react';
import { shallow } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import mockStore from 'autoru-frontend/mocks/mockStore';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';
import { getBunkerMock } from 'autoru-frontend/mockData/state/bunker.mock';

import cardMock from 'auto-core/react/dataDomain/card/mocks/card.cars.mock';
import userStateMock from 'auto-core/react/dataDomain/user/mocks';
import configStateMock from 'auto-core/react/dataDomain/config/mock';
import submitCallback from 'auto-core/react/dataDomain/dealerCallback/actions/submit';
import sendFrontLogAfterCallback from 'auto-core/react/dataDomain/dealerCallback/helpers/sendFrontLogAfterCallback';

import DealerCallback from './DealerCallback';
import type { OwnProps, ReduxState } from './DealerCallback';

const submitCallbackMock = submitCallback as jest.MockedFunction<typeof submitCallback>;

const submitPromise = Promise.resolve();
let context: typeof contextMock;
let initialState: ReduxState;
let props: OwnProps;
beforeEach(() => {
    initialState = {
        bunker: getBunkerMock([ 'common/callback_callcenter' ]),
        user: userStateMock.withAuth(false).value(),
        config: configStateMock.value(),
        searchID: {
            searchID: 'searchID',
            parentSearchId: 'parentSearchID',
        },
    };

    context = _.cloneDeep(contextMock);

    props = {
        hasSuggest: false,
        offer: cloneOfferWithHelpers(cardMock).value(),
    };

    submitCallbackMock.mockImplementation(() => {
        return () => submitPromise;
    });
});

describe('при успешной авторизации', () => {
    let phone: string;
    beforeEach(() => {
        phone = '89061234567';
        const wrapper = shallowRenderComponent();
        wrapper.find('Connect(LazyPhoneAuthAbstract)').simulate('authSuccess', phone);
    });

    it('должен отправить заявку', () => {
        expect(submitCallback).toHaveBeenCalledTimes(1);
        expect(submitCallback).toHaveBeenCalledWith({ enableTelephony: true, phone, offer: props.offer });
    });

    it('должен отправить метрику и фронт-лог', () => {
        return submitPromise
            .then(() => {
                expect(context.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
                expect(context.metrika.sendPageEvent).toHaveBeenCalledWith([ 'callback', 'submit' ]);

                expect(context.metrika.reachGoal).toHaveBeenCalledWith('CALLBACK_MODAL_CALL_REQUEST');

                expect(sendFrontLogAfterCallback).toHaveBeenCalled();
            });
    });
});

it('если это пользователь колл-центра, не будет запрашивать код подтверждения', () => {
    initialState.user.data.id = (initialState.bunker['common/callback_callcenter'] as Array<string>)[0];

    const wrapper = shallowRenderComponent();
    const lazyAuthComponent = wrapper.find('Connect(LazyPhoneAuthAbstract)');

    expect(lazyAuthComponent.prop('shouldRequestConfirmationCode')).toBe(false);
});

function shallowRenderComponent() {
    const store = mockStore(initialState);
    const ContextProvider = createContextProvider(context);

    const wrapper = shallow(
        <ContextProvider>
            <Provider store={ store }>
                <DealerCallback { ...props }/>
            </Provider>
        </ContextProvider>,
    );

    return wrapper.dive().dive().dive();
}
