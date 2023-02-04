import React from 'react';
import { render } from 'jest-puppeteer-react';
import merge from 'lodash/merge';
import { advanceTo } from 'jest-date-mock';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { IAppProviderProps } from 'realty-core/view/react/libs/test-helpers';

import { AppProvider } from 'view/lib/test-helpers';
import 'view/deskpad/common.css';

import { FinancesOperationsHistoryConteiner } from '../container';
import { IFinancesOperationsHistoryProps } from '../types';

import { store } from './stubs';

advanceTo(new Date('2021-10-28T20:59:59.999Z'));

const [WIDTH, HEIGHT] = [1000, 1000];

const getState = (stateOverrides = {}) => {
    return merge(store, stateOverrides);
};

interface IProps extends IFinancesOperationsHistoryProps, IAppProviderProps {}

const Component: React.FunctionComponent<Partial<IProps>> = ({ initialState, ...props }) => {
    return (
        <div style={{ padding: '20px' }}>
            <AppProvider initialState={initialState}>
                <FinancesOperationsHistoryConteiner {...props} />
            </AppProvider>
        </div>
    );
};

describe('FinancesOperationsHistory', () => {
    it('Базовая отрисовка', async () => {
        const store = getState();
        const component = <Component initialState={store} />;

        await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Раскрытие информации за день', async () => {
        const store = getState();
        const component = <Component initialState={store} />;

        await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

        await page.click('.operations-history__list > div:nth-child(2) > div');

        expect(await takeScreenshot()).toMatchImageSnapshot();

        for (let i = 1; i <= 5; i++) {
            await page.click(`.accordion__inner > div:nth-child(${i})`);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        }
    });
});
