import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { OffersSerpRecommendations } from '../';

import { initialState, getOffers } from './mocks';

const Component = () => (
    <AppProvider initialState={initialState}>
        <OffersSerpRecommendations
            items={getOffers()}
            pageParams={{}}
        />
    </AppProvider>
);

describe('OffersSerpRecommendations', () => {
    it('Рисует карусель рекомендаций', async() => {
        await render(
            <Component />,
            { viewport: { width: 360, height: 400 } }
        );
        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует карусель рекомендаций (горизонтально)', async() => {
        await render(
            <Component />,
            { viewport: { width: 700, height: 400 } }
        );
        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
