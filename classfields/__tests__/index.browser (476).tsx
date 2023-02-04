import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { EGRNPromoBanner } from '../';

describe('EGRNPromoBanner', () => {
    it('продажа', async () => {
        await render(
            <AppProvider>
                <EGRNPromoBanner type={'sell'} />
            </AppProvider>,
            { viewport: { width: 900, height: 250 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('аренда', async () => {
        await render(
            <AppProvider>
                <EGRNPromoBanner type={'rent'} />
            </AppProvider>,
            { viewport: { width: 900, height: 250 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
