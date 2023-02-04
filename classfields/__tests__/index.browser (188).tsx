import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';
import { FiltersNames, FilterValues } from 'realty-core/view/react/modules/developers-list/redux/types';

import { DevelopersListFilters } from '../index';
import filtersStyles from '../styles.module.css';

import { getInitialStateMock, getFiltersMock } from './mocks';

const viewports = [
    { width: 320, height: 400 },
    { width: 360, height: 400 },
    { width: 375, height: 400 },
] as const;

// eslint-disable-next-line @typescript-eslint/no-unused-vars
const onFilterValueChange = (name: FiltersNames, value: FilterValues) => {
    return;
};

const buildingClassDropdownSelector = `.${filtersStyles.control}:nth-of-type(2)`;

describe('DevelopersListFliters', () => {
    it('Рендерится корректно', async () => {
        for (const viewport of viewports) {
            await render(
                <AppProvider initialState={getInitialStateMock()}>
                    <DevelopersListFilters filters={getFiltersMock()} onFilterValueChange={onFilterValueChange} />
                </AppProvider>,
                { viewport }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        }
    });

    it('Корректно подставляет праметры', async () => {
        for (const viewport of viewports) {
            await render(
                <AppProvider initialState={getInitialStateMock()}>
                    <DevelopersListFilters filters={getFiltersMock(true)} onFilterValueChange={onFilterValueChange} />
                </AppProvider>,
                { viewport }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        }
    });

    it('Дизейблит недоступные классы жилья', async () => {
        for (const viewport of viewports) {
            await render(
                <AppProvider initialState={getInitialStateMock({ hasDisabledBuildingClasses: true })}>
                    <DevelopersListFilters filters={getFiltersMock()} onFilterValueChange={onFilterValueChange} />
                </AppProvider>,
                { viewport }
            );
            await page.click(buildingClassDropdownSelector);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        }
    });
});
