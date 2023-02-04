import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { OfferPhotoUpload } from '..';

import { state, noPhotosState } from './mocks';

describe('OfferPhotoUpload', () => {
    it('рисует компонент без фото', async () => {
        await render(
            <AppProvider initialState={noPhotosState}>
                <OfferPhotoUpload />
            </AppProvider>,
            { viewport: { width: 320, height: 600 } }
        );
        await page.addStyleTag({ content: 'body{padding: 0}' });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует компонент со всеми вариантами фото', async () => {
        await render(
            <AppProvider initialState={state}>
                <OfferPhotoUpload />
            </AppProvider>,
            {
                viewport: { width: 320, height: 600 },
            }
        );
        await page.addStyleTag({ content: 'body{padding: 0}' });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
