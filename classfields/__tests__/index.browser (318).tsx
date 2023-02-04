import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { SiteSnippetFlatTable } from '../';

import { siteCardMock } from './mock';

describe('SiteSnippetFlatTable', () => {
    it('Рендерится с полной квартирографией', async () => {
        await render(<SiteSnippetFlatTable item={siteCardMock} />, { viewport: { width: 360, height: 200 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
