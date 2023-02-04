import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { NavigationBar } from '../index';

import { initialState, initialStateWithoutSelectedFilters, props, mapProps } from './mocks';

const viewports = [
    { width: 320, height: 100 },
    { width: 375, height: 100 },
];

describe('NavigationBar', () => {
    viewports.forEach((viewport) => {
        it(`Рендерится корректно для листинга ${viewport.width}px`, async () => {
            await render(
                <AppProvider initialState={initialState}>
                    <NavigationBar {...props} />
                </AppProvider>,
                { viewport }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });

    viewports.forEach((viewport) => {
        it(`Рендерится корректно для карты ${viewport.width}px`, async () => {
            await render(
                <AppProvider initialState={initialState}>
                    <NavigationBar {...mapProps} />
                </AppProvider>,
                { viewport }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });

    viewports.forEach((viewport) => {
        it(`Отображение без выбранных фильтров ${viewport.width}px`, async () => {
            await render(
                <AppProvider initialState={initialStateWithoutSelectedFilters}>
                    <NavigationBar {...props} />
                </AppProvider>,
                { viewport }
            );
            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });
});
