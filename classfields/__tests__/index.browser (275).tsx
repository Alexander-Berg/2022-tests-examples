import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import OfferSerpTags from '../';

import fastlinks from './mock';

describe('OfferSerpTags', () => {
    it('Базовая отрисовка', async () => {
        await render(
            <AppProvider>
                <OfferSerpTags fastlinks={fastlinks} />
            </AppProvider>,
            { viewport: { width: 420, height: 140 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
