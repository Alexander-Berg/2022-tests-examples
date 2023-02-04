/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import React from 'react';
import { shallow } from 'enzyme';
import _ from 'lodash';

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import configStateMock from 'auto-core/react/dataDomain/config/mock';
import myReportsMock from 'auto-core/react/dataDomain/myReports/mocks/myReports.mock';

import ProAutoLanding from './ProAutoLanding';

let state: any;
let context: any;

global.scroll = jest.fn();

const props = {
    onSubmit: jest.fn(),
    isAuth: true,
    myReports: myReportsMock,
    noIframe: true,
    notify: {
        show: false,
        header: 'string',
        text: 'string',
    },
    reportsBundles: [],
};

beforeEach(() => {
    state = {
        bunker: {},
        config: configStateMock.value(),
        card: {},
        user: { data: { } },
        state: { authModal: {} },
        carCheck: {},
    };

    context = _.cloneDeep(contextMock);
});

describe('при заходе на страницу с action=scroll-to-reports', () => {
    const originalWindowLocation = global.location;
    const originalWindowHistoryReplaceState = global.history.replaceState;

    beforeEach(() => {
        global.location = { href: 'https://auto.ru/history/?action=scroll-to-reports' } as Location;
        global.history.replaceState = jest.fn();
    });

    afterEach(() => {
        global.location = originalWindowLocation;
        global.history.replaceState = originalWindowHistoryReplaceState;
    });

    it('должен удалить action из урла', () => {
        shallow(
            <ProAutoLanding { ...props }/>,
            { context: { ...context, store: mockStore(state) } },
        ).dive();

        expect(global.history.replaceState).toHaveBeenCalledWith(global.window.history.state, '', 'https://auto.ru/history/');
    });

    it('должен подскроллить к отчётам, если они есть', () => {
        (React.createRef as any) = () => ({ current: { scrollIntoView: jest.fn() } });

        const instance = shallow(
            <ProAutoLanding { ...props }/>,
            { context: { ...context, store: mockStore(state) } },
        ).dive().instance();

        expect((instance as any).contentRef.current.scrollIntoView).toHaveBeenCalled();
        expect((instance as any).topRef.current.scrollIntoView).not.toHaveBeenCalled();
    });

    it('должен подскроллить к блоку со строкой поиска, если отчётов есть', () => {
        (React.createRef as any) = () => ({ current: { scrollIntoView: jest.fn() } });

        const instance = shallow(
            <ProAutoLanding { ...props } myReports={{ ...myReportsMock, reports: [] }}/>,
            { context: { ...context, store: mockStore(state) } },
        ).dive().instance();

        expect((instance as any).contentRef.current.scrollIntoView).not.toHaveBeenCalled();
        expect((instance as any).topRef.current.scrollIntoView).toHaveBeenCalled();
    });
});
