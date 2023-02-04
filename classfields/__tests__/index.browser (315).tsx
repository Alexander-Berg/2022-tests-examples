import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { SiteSecondarySearch } from '..';

import { getSiteCard } from './mocks';

describe('SiteSecondarySearch', () => {
    it('рендерится корректно', async () => {
        await render(
            <AppProvider>
                <SiteSecondarySearch card={getSiteCard()} linkUrl="mock" />
            </AppProvider>,
            {
                viewport: { width: 320, height: 250 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
