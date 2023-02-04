import React from 'react';

import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { yandexMapsSiteWidgetPageReducer } from 'view/reducers/roots/yandex-maps-site-widget';

import { YandexMapsSiteWidget } from '../index';

import { card, genPlan, developerSites, getOffersGate } from './mocks';

const initialState = {
    cookies: {},
    config: {
        constants: {},
    },
    newbuildingCardPage: {
        card,
    },
    genPlan,
    similar: {
        'developer-sites': developerSites,
    },
    user: {
        favorites: ['site_122308'],
        favoritesMap: {
            site_122308: true,
        },
    },
};

describe('YandexMapsSiteWidget', () => {
    it('рендерится корректно', async () => {
        await render(
            <AppProvider initialState={initialState} Gate={getOffersGate} rootReducer={yandexMapsSiteWidgetPageReducer}>
                <YandexMapsSiteWidget />
            </AppProvider>,
            {
                viewport: { width: 360, height: 500 },
            }
        );

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });
});
