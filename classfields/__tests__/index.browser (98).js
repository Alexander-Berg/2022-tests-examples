import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import rootReducer from 'view/reducers/roots/widget-site-offers';

import WidgetSiteOffers from '..';

import { getInitialState, gateSiteOffers, gateSiteOffersStats } from './mocks';

const Component = () => {
    const Gate = {
        get: action => {
            if (action === 'site-offers.getOffers') {
                return Promise.resolve(gateSiteOffers);
            }
            if (action === 'site-offers.getStats') {
                return Promise.resolve(gateSiteOffersStats);
            }
        }
    };

    return (
        <AppProvider initialState={getInitialState()} Gate={Gate} rootReducer={rootReducer}>
            <WidgetSiteOffers />
        </AppProvider>
    );
};

describe('WidgetSiteOffers', () => {
    it('[Темная тема] рисует квартирогрфию в свернутом состоянии', async() => {
        await render(
            <Component />,
            { viewport: { width: 320, height: 500 } }
        );

        await page.addStyleTag({ content: 'body{padding: 0}' });

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('[Темная тема] рисует квартирогрфию в развернутом состоянии', async() => {
        await render(
            <Component />,
            { viewport: { width: 320, height: 500 } }
        );

        await page.addStyleTag({ content: 'body{padding: 0}' });

        await page.click('.NewbuildingOffersStat__row');

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });
});
