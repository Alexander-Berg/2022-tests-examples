import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';
import { ApartmentType } from 'realty-core/types/siteCard';

import { SitePlansOffersSerp } from '..';

import { getProps, getInitialState } from './mocks';

describe('SitePlansOffersSerp', () => {
    it('рисует выдачу с планировкой', async () => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <SitePlansOffersSerp {...getProps(ApartmentType.FLATS, true)} />
            </AppProvider>,
            { viewport: { width: 320, height: 400 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует выдачу с планировкой и открытой сортировкой', async () => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <SitePlansOffersSerp {...getProps(ApartmentType.FLATS, true)} />
            </AppProvider>,
            { viewport: { width: 320, height: 400 } }
        );

        await page.click('.Select');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует выдачу без планировки квартиры', async () => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <SitePlansOffersSerp {...getProps(ApartmentType.FLATS)} />
            </AppProvider>,
            { viewport: { width: 320, height: 400 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует выдачу без планировки апартаменты', async () => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <SitePlansOffersSerp {...getProps(ApartmentType.APARTMENTS)} />
            </AppProvider>,
            { viewport: { width: 320, height: 400 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует выдачу без планировки квартиры и апартаменты', async () => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <SitePlansOffersSerp {...getProps(ApartmentType.APARTMENTS_AND_FLATS)} />
            </AppProvider>,
            { viewport: { width: 320, height: 400 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует выдачу c картинками планировок', async () => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <SitePlansOffersSerp {...getProps(ApartmentType.FLATS)} withPlanImages />
            </AppProvider>,
            { viewport: { width: 320, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
