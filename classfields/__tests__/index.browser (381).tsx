import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { OfferGalleryArendaBanner } from '../';

describe('OfferGalleryArendaBanner', () => {
    it('рендерится корректно', async () => {
        await render(
            <AppProvider initialState={{ config: { yaArendaUrl: '' } }}>
                <OfferGalleryArendaBanner />
            </AppProvider>,
            { viewport: { width: 400, height: 400 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
