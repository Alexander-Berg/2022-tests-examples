import React from 'react';
import Enzyme, { mount } from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';
import { combineReducers, Reducer } from 'redux';
import { Provider } from 'react-redux';
import { all } from 'redux-saga/effects';
import SagaTester from 'redux-saga-tester';
import { fetchGet } from 'common/utils/old-fetch';

jest.mock('common/utils/old-fetch');
jest.mock('common/history/client');
jest.mock('../history');

import { HOST } from 'common/utils/test-utils/common';
import commonReducers from 'common/reducers/common';
import withIntlProvider from 'common/utils/test-utils/with-intl-provider';
import { reducers } from '../reducers';
import { getInitialState as getCommonInitialState } from 'common/reducers/client';
import { getInitialState } from '../reducers/list';
import { watchFetchInitialData } from '../sagas/initial';
import { fromQSToState } from '../history';
import { fromQSToState as fromQSToStateCommon } from 'common/history/client';
import { DISCOUNT_ACTION } from '../actions';
import { RootContainer } from '../containers/RootContainer';
import { perms, initialData } from './data';
import { initializeDesktopRegistry } from 'common/__tests__/registry';

function* rootSaga() {
    yield all([watchFetchInitialData()]);
}

Enzyme.configure({ adapter: new Adapter() });

describe('admin-discounts-root', () => {
    beforeAll(initializeDesktopRegistry);

    afterEach(() => {
        jest.resetAllMocks();
    });

    test('open page - should request initial data', async () => {
        expect.assertions(2);

        // @ts-ignore
        fromQSToStateCommon.mockReturnValue({ clientDetails: { id: 1 } });
        // @ts-ignore
        fromQSToState.mockReturnValue({ clientId: 1 });
        // @ts-ignore
        fetchGet.mockResolvedValueOnce(initialData);

        const initialState = {
            perms,
            client: getCommonInitialState(),
            list: getInitialState()
        };

        const rootReducer: Reducer = combineReducers({
            ...commonReducers,
            ...reducers
        });

        const sagaTester = new SagaTester({
            initialState,
            reducers: rootReducer
        });

        sagaTester.start(rootSaga);

        // @ts-ignore
        const store = sagaTester.store;

        const Container = withIntlProvider(() => (
            <Provider store={store}>
                <RootContainer />
            </Provider>
        ));

        const wrapper = mount(<Container />);

        // Отправляем запрос на получение данных по скидкам клиента
        await sagaTester.waitFor(DISCOUNT_ACTION.REQUEST);

        expect(fetchGet).toBeCalledWith(`${HOST}/client/discount`, { client_id: 1 }, false, false);

        await sagaTester.waitFor(DISCOUNT_ACTION.RECEIVE);

        wrapper.update();

        const listItems = wrapper.find('List').find('PageSection').find('tbody').find('tr');

        expect(listItems.length).toBe(initialData.data.length);
    });
});
