import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { SitePlansSecondarySearch } from '..';

import { getSiteCard } from './mocks';

describe('SitePlansSecondarySearch', () => {
    it('рендерится корректно', async () => {
        await render(
            <AppProvider>
                <SitePlansSecondarySearch card={getSiteCard()} linkUrl="mock" />
            </AppProvider>,
            {
                viewport: { width: 1000, height: 200 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
