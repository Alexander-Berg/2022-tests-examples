import React from 'react';
import Enzyme, { mount } from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';
import SagaTester from 'redux-saga-tester';
import { combineReducers, Reducer } from 'redux';
import { Provider } from 'react-redux';
import { fetchGet } from 'common/utils/old-fetch';

import { fromQSToState } from 'common/history/client';
import { getLang } from 'common/utils/body-data';
import { request } from 'common/utils/request';
import { client, currencies, intercompanies, regions } from './data';
import commonReducers from 'common/reducers/common';
import { reducers } from '../reducers';
import { rootSaga } from '../sagas';

jest.mock('common/utils/old-fetch');
jest.mock('common/utils/body-data');
jest.mock('common/utils/request');
jest.mock('common/history/client');

import withIntlProvider from 'common/utils/test-utils/with-intl-provider';
import { RootContainer } from '../containers/RootContainer';
import { InitialDataAction } from 'common/actions';
import { Permissions } from 'common/constants';
import { initializeDesktopRegistry } from 'common/__tests__/registry';

Enzyme.configure({ adapter: new Adapter() });

window.React = React;

describe('admin - editclient', () => {
    beforeAll(initializeDesktopRegistry);

    afterEach(() => {
        jest.resetAllMocks();
    });

    it('подгрузка начальных данных', async () => {
        expect.assertions(6);
        // настроим все моки
        (getLang as jest.Mock).mockReturnValue('ru');
        (fromQSToState as jest.Mock).mockReturnValue({
            clientDetails: {
                id: 12345
            }
        });
        (fetchGet as jest.Mock).mockResolvedValueOnce(regions.response);
        (request.get as jest.Mock).mockResolvedValueOnce(client.response);
        (request.get as jest.Mock).mockResolvedValueOnce(intercompanies.response);
        (request.get as jest.Mock).mockResolvedValueOnce(currencies.response);

        const rootReducer: Reducer = combineReducers({
            ...commonReducers,
            ...reducers
        });

        const sagaTester = new SagaTester<object>({
            initialState: {
                perms: [Permissions.EDIT_CLIENT]
            },
            reducers: rootReducer,
            middlewares: []
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

        await sagaTester.waitFor(InitialDataAction.RECEIVE);
        wrapper.update();

        expect(fetchGet).toHaveBeenCalledTimes(1);
        expect(fetchGet).nthCalledWith(1, ...regions.request);
        expect(request.get).toHaveBeenCalledTimes(3);
        expect(request.get).nthCalledWith(1, client.request);
        expect(request.get).nthCalledWith(2, intercompanies.request);
        expect(request.get).nthCalledWith(3, currencies.request);
    });
});
