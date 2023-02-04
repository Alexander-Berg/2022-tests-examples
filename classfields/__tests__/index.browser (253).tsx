import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { NewbuildingReviewsContainer } from '../container';

import { initialState } from './mocks';

describe('NewbuildingReviews', () => {
    it('рендерится корректно', async () => {
        await render(
            <AppProvider initialState={initialState}>
                <NewbuildingReviewsContainer />
            </AppProvider>,
            { viewport: { width: 345, height: 200 } }
        );

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('открывается форма добавления отзыва', async () => {
        await render(
            <AppProvider initialState={initialState}>
                <NewbuildingReviewsContainer />
            </AppProvider>,
            { viewport: { width: 345, height: 200 } }
        );

        await page.click('.Reviews__add');

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });
});
