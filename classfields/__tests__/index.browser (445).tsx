import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { NewbuildingMortgageContainer } from '../container';

import { state, stateWithFallback } from './mocks';

describe('NewbuildingMortgage', () => {
    it('рендерится корректно', async () => {
        await render(
            <AppProvider initialState={state}>
                <NewbuildingMortgageContainer />
            </AppProvider>,
            { viewport: { width: 1200, height: 900 } }
        );

        expect(await takeScreenshot({ fullPage: true, keepCursor: true })).toMatchImageSnapshot();
    });

    it('рисует текст фоллбэка для программ', async () => {
        await render(
            <AppProvider initialState={stateWithFallback}>
                <NewbuildingMortgageContainer />
            </AppProvider>,
            { viewport: { width: 1200, height: 900 } }
        );

        expect(await takeScreenshot({ fullPage: true, keepCursor: true })).toMatchImageSnapshot();
    });
});
