import React from 'react';
import { mount, ShallowWrapper } from 'enzyme';
import { combineReducers, Reducer } from 'redux';
import { Provider } from 'react-redux';
import { all } from 'redux-saga/effects';
import SagaTester from 'redux-saga-tester';
import { Record } from 'immutable';

import { request } from 'common/utils/request';
import commonReducers from 'common/reducers/common';
import withIntlProvider from 'common/utils/test-utils/with-intl-provider';

import { rootReducer as pageReducers } from '../reducers';
import { ContractsContainer } from '../containers/ContractsContainer';
import { RestrictionsContainer } from '../containers/RestrictionsContainer';
import { watchContracts } from '../sagas/contracts';
import { watchRestrictions } from '../sagas/restrictions';
import { CreditsState } from '../types';
import { CreditsStateRecord } from '../reducers/credits';
import { ContractsAction, RestrictionsAction } from '../constants';

window.React = React;

type InitialState = {
    credits: Record<CreditsState>;
};

interface MockData {
    request: (object | string | boolean | undefined)[] | object;
    response?: object | null;
    error?: any;
}

export class Page {
    wrapper: ShallowWrapper;
    sagaTester: SagaTester<InitialState>;
    request: jest.Mock;

    constructor(data: { contracts: MockData; activityTypes: MockData; restrictions: MockData }) {
        function* rootSaga() {
            yield all([watchContracts(), watchRestrictions()]);
        }

        const initialState = {
            credits: CreditsStateRecord({
                clientId: 123
            })
        };
        const rootReducer: Reducer = combineReducers({
            ...commonReducers,
            ...pageReducers
        });

        this.request = request.get as jest.Mock;
        this.request
            .mockResolvedValueOnce(data.contracts.response)
            .mockResolvedValueOnce(data.activityTypes.response)
            .mockResolvedValueOnce(data.restrictions.response);

        const sagaTester = new SagaTester<typeof initialState>({
            initialState,
            reducers: rootReducer,
            middlewares: []
        });

        sagaTester.start(rootSaga);

        // @ts-ignore
        const store = sagaTester.store;

        const Container = withIntlProvider(() => (
            <Provider store={store}>
                <ContractsContainer />
                <RestrictionsContainer />
            </Provider>
        ));

        this.sagaTester = sagaTester;
        // @ts-ignore
        this.wrapper = mount(<Container />);
    }

    async initializePage() {
        await this.sagaTester.waitFor(ContractsAction.RECEIVE);
        await this.sagaTester.waitFor(RestrictionsAction.RECEIVE);
        this.wrapper.update();
    }

    getCreditItems() {
        return this.wrapper.find('Credit').find('tbody').find('tr');
    }

    getRestrictionItems() {
        return this.wrapper.find('RestrictionMessages').find('ul').find('li');
    }
}
