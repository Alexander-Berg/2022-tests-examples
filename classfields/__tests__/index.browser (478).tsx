import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { OffersSerpArendaPromo } from '../';

const Component = () => (
    <AppProvider>
        <OffersSerpArendaPromo />
    </AppProvider>
);

describe('OffersSerpArendaPromo', () => {
    it('Базовая отрисовка', async () => {
        await render(<Component />, { viewport: { width: 900, height: 300 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await render(<Component />, { viewport: { width: 960, height: 300 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
