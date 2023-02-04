import React from 'react';
import { shallow } from 'enzyme';
import _ from 'lodash';

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import MyVinReports from './MyVinReports';

let context: typeof contextMock;

beforeEach(() => {
    context = _.cloneDeep(contextMock);
});

it('отправляет метрику my_reports no_reports', () => {
    const store = mockStore({
        bunker: {},
        myReports: {
            reports: [],
            paging: {},
        },
    });
    shallow(
        <MyVinReports
            renderBefore={ () => null }
            renderItem={ () => null }
        />, { context: { ...context, store } },
    ).dive();

    expect(context.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
    expect(context.metrika.sendPageEvent.mock.calls[0][0]).toEqual([ 'my_reports', 'no_reports' ]);
});

it('отправляет метрику my_reports has_reports', () => {
    const store = mockStore({
        bunker: {},
        myReports: {
            reports: [ {} ],
            paging: {
                page: {
                    num: 1,
                    page_count: 1,
                },
            },
        },
    });
    shallow(
        <MyVinReports
            renderBefore={ () => null }
            renderItem={ () => null }
        />, { context: { ...context, store } },
    ).dive();

    expect(context.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
    expect(context.metrika.sendPageEvent.mock.calls[0][0]).toEqual([ 'my_reports', 'has_reports' ]);
});
