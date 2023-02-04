import React from 'react';
import Enzyme, { mount } from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';
import { combineReducers } from 'redux';
import { Provider } from 'react-redux';
import { all } from 'redux-saga/effects';
import SagaTester from 'redux-saga-tester';
import { fetchGet } from 'common/utils/old-fetch';

import withIntlProvider from 'common/utils/test-utils/with-intl-provider';

import { ListContainer } from '../ListContainer';
import reducers from '../../reducers';
import { watchItemsRequest } from '../../sagas/cart';
import { CART } from '../../actions';
import { initializeDesktopRegistry } from 'common/__tests__/registry';

jest.mock('common/utils/old-fetch');
jest.mock('../../history-utils');

Enzyme.configure({ adapter: new Adapter() });

const HOST = 'http://snout-test';

describe('cart list container', () => {
    let sagaTester;
    let Container;

    beforeAll(initializeDesktopRegistry);

    beforeEach(() => {
        function* rootSaga() {
            yield all([watchItemsRequest()]);
        }

        const initialState = combineReducers(reducers)(undefined, {});

        sagaTester = new SagaTester({
            initialState,
            reducers: combineReducers(reducers),
            middlewares: []
        });

        sagaTester.start(rootSaga);

        Container = withIntlProvider(() => {
            return (
                <Provider store={sagaTester.store}>
                    <ListContainer
                        DeleteButtonComponent={() => {
                            return null;
                        }}
                    />
                </Provider>
            );
        });
    });

    afterEach(() => {
        jest.resetAllMocks();
    });

    test('calls API to fetch the data', async () => {
        expect.assertions(1);

        fetchGet.mockResolvedValueOnce({ data: { items: [], invoices: [] } });

        const wrapper = mount(<Container />);

        await sagaTester.waitFor(CART.REQUEST);

        expect(fetchGet).toBeCalledWith(
            `${HOST}/cart/item/list`,
            {
                detailed: true,
                service_id: '42'
            },
            false,
            false
        );
    });
});
