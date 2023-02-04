import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { FavoriteSerpTabs } from '../';

describe('FavoriteSerpTabs', () => {
    it('рендерится корректно', async () => {
        await render(<FavoriteSerpTabs favoriteType="OFFER" onChange={() => undefined} />, {
            viewport: { width: 700, height: 150 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
