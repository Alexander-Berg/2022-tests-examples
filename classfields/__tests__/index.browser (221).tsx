import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { FavoriteSerpTabs } from '../';

describe('FavoritesSerpTabs', () => {
    it('рендерится корректно', async () => {
        await render(<FavoriteSerpTabs onChange={() => undefined} />, { viewport: { width: 345, height: 100 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
