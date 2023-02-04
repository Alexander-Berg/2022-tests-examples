import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { DevelopersListFilters } from '../index';
import styles from '../styles.module.css';

import { getInitialStateMock, getComponentProps } from './mocks';

describe('DevelopersListFliters', () => {
    it('Рендерится корректно', async () => {
        await render(
            <AppProvider initialState={getInitialStateMock()}>
                <DevelopersListFilters {...getComponentProps()} />
            </AppProvider>,
            { viewport: { width: 1000, height: 400 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Корректно подставляет праметры', async () => {
        await render(
            <AppProvider initialState={getInitialStateMock()}>
                <DevelopersListFilters {...getComponentProps({ hasParams: true })} />
            </AppProvider>,
            { viewport: { width: 1000, height: 400 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Дизейблит недоступные классы жилья', async () => {
        await render(
            <AppProvider initialState={getInitialStateMock({ hasDisabledBuildingClasses: true })}>
                <DevelopersListFilters {...getComponentProps()} />
            </AppProvider>,
            { viewport: { width: 1000, height: 400 } }
        );
        await page.click(`.${styles.buildingClassSelect}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Корректно сбрасывает параметры', async () => {
        await render(
            <AppProvider initialState={getInitialStateMock()}>
                <DevelopersListFilters {...getComponentProps({ hasParams: true })} />
            </AppProvider>,
            { viewport: { width: 1000, height: 400 } }
        );
        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(`.${styles.clearBtn}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
