import React from 'react';
import Enzyme, { mount } from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';
import { combineReducers } from 'redux';
import { Provider } from 'react-redux';
import { all } from 'redux-saga/effects';
import SagaTester from 'redux-saga-tester';
import { fetchPost } from 'common/utils/old-fetch';

import withIntlProvider from 'common/utils/test-utils/with-intl-provider';
import { Button } from 'common/lego-components';

import DeleteButtonContainer from '../DeleteButtonContainer';
import reducers from '../../reducers';
import { watchItemDeleteRequest } from '../../sagas/cart';
import { CART } from '../../actions';
import { initializeDesktopRegistry } from 'common/__tests__/registry';

jest.mock('common/utils/old-fetch');
jest.mock('../../history-utils');

Enzyme.configure({ adapter: new Adapter() });

const HOST = 'http://snout-test';

describe('cart delete-button container', () => {
    let sagaTester;
    let Container;

    beforeAll(initializeDesktopRegistry);

    beforeEach(() => {
        function* rootSaga() {
            yield all([watchItemDeleteRequest()]);
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
                    <DeleteButtonContainer id={2} />
                </Provider>
            );
        });
    });

    afterEach(() => {
        jest.resetAllMocks();
    });

    test.only('sends item id with csrf to API on confirm', async () => {
        expect.assertions(2);

        fetchPost.mockResolvedValueOnce({ data: { items: [] } });

        const wrapper = mount(<Container />);
        wrapper.update();

        wrapper.find('button.delete-button').simulate('click');
        wrapper.update();

        expect(wrapper.find('button').length).toEqual(3);

        wrapper.find('button').at(2).simulate('click');

        await sagaTester.waitFor(CART.DELETE_ITEM);

        expect(fetchPost).toBeCalledWith(
            `${HOST}/cart/item/delete`,
            {
                item_ids: '2',
                _csrf: 'csrf',
                service_id: '42'
            },
            false
        );

        await sagaTester.waitFor(CART.DELETE_ITEM_SUCCESS);
    });
});
