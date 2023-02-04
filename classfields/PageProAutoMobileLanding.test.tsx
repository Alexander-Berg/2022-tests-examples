/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import React from 'react';
import { shallow } from 'enzyme';
import _ from 'lodash';

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import configStateMock from 'auto-core/react/dataDomain/config/mock';

import PageProAutoMobileLanding from './PageProAutoMobileLanding';

let state: any;
let context: any;

global.scroll = jest.fn();

beforeEach(() => {
    state = {
        bunker: {},
        config: configStateMock.value(),
        card: {},
        user: { data: { } },
        state: { authModal: {} },
    };

    context = _.cloneDeep(contextMock);
});

describe('метрики', () => {
    it('отправляем метрику показа лендинга', () => {
        const store = mockStore(state);
        shallow(
            <PageProAutoMobileLanding/>, { context: { ...context, store } },
        ).dive();

        expect(context.metrika.sendPageEvent).toHaveBeenCalled();
        expect(context.metrika.sendPageEvent.mock.calls[0][0]).toEqual([ 'landing', 'view' ]);
    });

    it('отправляет метрику поиска', () => {
        const store = mockStore(state);
        const wrapper = shallow(
            <PageProAutoMobileLanding/>, { context: { ...context, store } },
        ).dive();

        wrapper.find('Connect(ProAutoLanding)').simulate('submit', 'B111BB11');

        expect(context.metrika.sendPageEvent).toHaveBeenCalledTimes(2);
        expect(context.metrika.sendPageEvent.mock.calls[1][0]).toEqual([ 'landing', 'perform_search' ]);
    });
});
