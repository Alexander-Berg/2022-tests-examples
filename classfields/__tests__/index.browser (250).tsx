import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { NewbuildingMortgageContainer } from '../container';

import { state, stateHandOverCard, stateConstructionSuspended, stateWithFallback, fakeTimers } from './mocks';

describe('NewbuildingMortgage', () => {
    it('рендерится корректно', async () => {
        await render(
            <AppProvider fakeTimers={fakeTimers} initialState={state}>
                <NewbuildingMortgageContainer />
            </AppProvider>,
            { viewport: { width: 345, height: 3500 } }
        );

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('рендерится корректно дом сдан', async () => {
        await render(
            <AppProvider fakeTimers={fakeTimers} initialState={stateHandOverCard}>
                <NewbuildingMortgageContainer />
            </AppProvider>,
            { viewport: { width: 345, height: 3500 } }
        );

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('рендерится корректно стройка заморожена', async () => {
        await render(
            <AppProvider fakeTimers={fakeTimers} initialState={stateConstructionSuspended}>
                <NewbuildingMortgageContainer />
            </AppProvider>,
            { viewport: { width: 345, height: 3500 } }
        );

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('рисует текст фоллбэка для программ', async () => {
        await render(
            <AppProvider fakeTimers={fakeTimers} initialState={stateWithFallback}>
                <NewbuildingMortgageContainer />
            </AppProvider>,
            { viewport: { width: 345, height: 500 } }
        );

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });
});
