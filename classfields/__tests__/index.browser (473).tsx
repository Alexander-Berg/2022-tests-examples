import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { SerpRelatedNewbuildings } from '../';

import { mock } from './mock';

describe('SerpRelatedNewbuildings', () => {
    it('рендерится в дефолтном состоянии', async () => {
        await render(
            <AppProvider>
                <SerpRelatedNewbuildings siteSnippetList={mock} searchParams={{}} eventPlace="unknown" />
            </AppProvider>,
            { viewport: { width: 1000, height: 500 } }
        );

        await page.hover('h3');

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('рендерится после листания вправо', async () => {
        await render(
            <AppProvider>
                <SerpRelatedNewbuildings siteSnippetList={mock} searchParams={{}} eventPlace="unknown" />
            </AppProvider>,
            { viewport: { width: 1000, height: 500 } }
        );

        await page.click('.SwipeableBlock__forward');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
