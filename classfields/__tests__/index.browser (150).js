import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import rootReducer from 'view/react/deskpad/reducers/roots/widget-site-offers';

import WidgetSiteOffers from '..';

import { getInitialState, gateSitePlans, gateSitePlansStats } from './mocks';

const Component = () => {
    const Gate = {
        get: action => {
            if (action === 'site-plans.getPlans') {
                return Promise.resolve(gateSitePlans);
            }
            if (action === 'site-plans.getStats') {
                return Promise.resolve(gateSitePlansStats);
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
            { viewport: { width: 1280, height: 800 } }
        );

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('[Темная тема] рисует квартирогрфию в развернутом состоянии', async() => {
        await render(
            <Component />,
            { viewport: { width: 1280, height: 800 } }
        );

        await page.click('[data-test=SitePlansGroupedListRow]');

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });
});
