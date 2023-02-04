import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { SitePlansFilters } from '..';
import styles from '../styles.module.css';

import { getInitialState } from './mocks';

const noop = () => {};

describe('SitePlansFilters', () => {
    it('рисует закрытое состояние фильтра', async() => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <SitePlansFilters onMount={noop} />
            </AppProvider>,
            { viewport: { width: 360, height: 330 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует состояние фильтра без доступных корпусов', async() => {
        await render(
            <AppProvider initialState={getInitialState({ houseIdValues: [] })}>
                <SitePlansFilters onMount={noop} />
            </AppProvider>,
            { viewport: { width: 360, height: 330 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует закрытое состояние фильтра с возможностью сброса', async() => {
        await render(
            <AppProvider initialState={getInitialState({ data: { priceType: 'PER_METER' } })}>
                <SitePlansFilters onMount={noop} />
            </AppProvider>,
            { viewport: { width: 360, height: 330 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует открытое состояние фильтра', async() => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <SitePlansFilters onMount={noop} />
            </AppProvider>,
            { viewport: { width: 360, height: 1100 } }
        );

        await page.click(`.${styles.showParamsButton}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует открытое состояние фильтра с возможностью сброса', async() => {
        await render(
            <AppProvider initialState={getInitialState({ data: { priceType: 'PER_METER' }, matchedQuantity: 10 })}>
                <SitePlansFilters onMount={noop} />
            </AppProvider>,
            { viewport: { width: 360, height: 1100 } }
        );

        await page.click(`.${styles.showParamsButton}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
