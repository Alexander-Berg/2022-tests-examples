import React from 'react';
import Enzyme, { mount } from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';
import { combineReducers } from 'redux';
import { Provider } from 'react-redux';
import { all } from 'redux-saga/effects';
import SagaTester from 'redux-saga-tester';
import { fetchPost } from 'common/utils/old-fetch';

import withIntlProvider from 'common/utils/test-utils/with-intl-provider';

import { ConfirmContainer } from '../ConfirmContainer';
import reducers from '../../reducers';
import { watchCreateRequest } from '../../sagas/cart';
import { CART } from '../../actions';
import { initializeDesktopRegistry } from 'common/__tests__/registry';

jest.mock('common/utils/old-fetch');
jest.mock('../../history-utils');

Enzyme.configure({ adapter: new Adapter() });

const HOST = 'http://snout-test';

describe('cart confirm container', () => {
    let sagaTester;
    let Container;

    beforeAll(initializeDesktopRegistry);

    beforeEach(() => {
        function* rootSaga() {
            yield all([watchCreateRequest()]);
        }

        const initialState = combineReducers(reducers)(undefined, {});

        initialState.cart = initialState.cart.update('items', items => {
            return items
                .push({ id: 1, active: true })
                .push({ id: 2, active: true })
                .push({ id: 3, active: false })
                .push({ id: 4, active: true });
        });

        sagaTester = new SagaTester({
            initialState,
            reducers: combineReducers(reducers),
            middlewares: []
        });

        sagaTester.start(rootSaga);

        Container = withIntlProvider(() => {
            return (
                <Provider store={sagaTester.store}>
                    <ConfirmContainer />
                </Provider>
            );
        });
    });

    describe('create request', () => {
        afterEach(() => {
            jest.resetAllMocks();
        });

        test('sends active item ids with csrf to API on submit', async () => {
            expect.assertions(1);

            fetchPost.mockResolvedValueOnce({ data: {} });

            const wrapper = mount(<Container />);

            wrapper.find('form').simulate('submit');

            await sagaTester.waitFor(CART.CREATE_REQUEST);

            expect(fetchPost).toBeCalledWith(
                `${HOST}/cart/create-request`,
                {
                    item_ids: '1,2,4',
                    _csrf: 'csrf',
                    service_id: '42'
                },
                false
            );

            await sagaTester.waitFor(CART.CREATE_REQUEST_SUCCESS);
        });
    });
});
