import React from 'react';
import Enzyme, { mount } from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';
import { combineReducers, Reducer } from 'redux';
import { Provider } from 'react-redux';
import { all } from 'redux-saga/effects';
import SagaTester from 'redux-saga-tester';

import { request } from 'common/utils/request';
import commonReducers from 'common/reducers/common';
import withIntlProvider from 'common/utils/test-utils/with-intl-provider';
import { InitialDataAction } from 'common/actions';
import { clientSelectorState as initialClientSelectorState } from 'common/reducers/client-selector';
import { Permissions } from 'common/constants';

import { RootContainer } from '../RootContainer';
import reducers from '../../reducers';
import { watchRequestInitialData, watchHumarizeData } from '../../sagas/filter';
import { getInitialState as getInitialFilter } from '../../reducers/filter';
import { getInitialList } from '../../reducers/list';
import { initialState as root } from '../../reducers/root';
import { initializeDesktopRegistry } from 'common/__tests__/registry';

jest.mock('common/utils/request');

Enzyme.configure({ adapter: new Adapter() });

const HOST = 'http://snout-test';

describe('acts root', () => {
    beforeAll(initializeDesktopRegistry);

    describe('fetching data', () => {
        afterEach(() => {
            jest.resetAllMocks();
        });

        test('fetch initial', async () => {
            expect.assertions(6);

            function* rootSaga() {
                yield all([watchRequestInitialData(), watchHumarizeData()]);
            }

            const initialState = {
                root,
                perms: [Permissions.NEW_UI_EARLY_ADOPTER],
                filter: getInitialFilter(),
                list: getInitialList(),
                clientSelector: initialClientSelectorState
            };

            const sagaTester = new SagaTester({
                initialState,
                reducers: combineReducers({
                    ...reducers,
                    ...commonReducers
                }) as Reducer,
                middlewares: []
            });

            sagaTester.start(rootSaga);

            // @ts-ignore
            const store = sagaTester.store;

            let Container = () => (
                <Provider store={store}>
                    <RootContainer />
                </Provider>
            );

            Container = withIntlProvider(Container);

            (request.get as jest.Mock)
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([]);

            mount(<Container />);

            await sagaTester.waitFor(InitialDataAction.RECEIVE);

            expect(request.get).toBeCalledWith({ url: `${HOST}/currency/list` });
            expect(request.get).toBeCalledWith({ url: `${HOST}/firm/list` });
            expect(request.get).toBeCalledWith({ url: `${HOST}/service/list`, data: {} });

            expect(sagaTester.getState().filter.currencies.toJS()).toEqual([]);
            expect(sagaTester.getState().filter.firms.toJS()).toEqual([]);
            expect(sagaTester.getState().filter.services.toJS()).toEqual([]);
        });
    });
});
