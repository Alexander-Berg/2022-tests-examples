import React from 'react';
import { render } from 'jest-puppeteer-react';
import merge from 'lodash/merge';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { IAppProviderProps } from 'realty-core/view/react/libs/test-helpers';

import { AppProvider } from 'view/lib/test-helpers';
import 'view/deskpad/common.css';

import { DashboardWidgetTotalStatContainer } from '../container';

const [WIDTH, HEIGHT] = [600, 400];

const getState = (stateOverrides = {}) => {
    return merge(
        {
            callsHistory: {
                status: 'loaded',
                statsByDay: [
                    {
                        interval: {
                            from: '2020-08-03T21:00:00Z',
                            to: '2020-08-04T21:00:00Z',
                        },
                        stat: {
                            success: 0,
                            target: 0,
                            nonTarget: 0,
                            missed: 0,
                            blocked: 0,
                            payedTuz: 0,
                        },
                    },
                ],
            },
            periodFilters: {
                startTime: '2020-07-31T21:00:00.000Z',
                endTime: '2020-08-31T20:59:59.999Z',
            },
        },
        stateOverrides
    );
};

const Component: React.FunctionComponent<Partial<IAppProviderProps>> = ({ initialState }) => (
    <div style={{ padding: '20px' }}>
        <AppProvider initialState={initialState}>
            <DashboardWidgetTotalStatContainer />
        </AppProvider>
    </div>
);

describe('DashboardWidgetTotalStatContainer', () => {
    it('Отрисовка нулевой статы', async () => {
        const store = getState();
        const component = <Component initialState={store} />;

        await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка статы с большими числами', async () => {
        const store = getState();

        store.callsHistory.statsByDay[0].stat.success = 123456789101234;
        store.callsHistory.statsByDay[0].stat.blocked = 123456789101234;
        store.callsHistory.statsByDay[0].stat.payedTuz = 1234;

        const component = <Component initialState={store} />;

        await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Ошибка загрузки статистики', async () => {
        const store = getState({
            callsHistory: {
                status: 'errored',
            },
        });
        const component = <Component initialState={store} />;

        await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
