import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import DeveloperCardRewards from '../';

import { developerWithRewards } from './mocks';

describe('DeveloperCardRewards', () => {
    it('рисует картинки наград', async() => {
        await render(<DeveloperCardRewards developer={developerWithRewards} />,
            { viewport: { width: 600, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует тултип с информацией о награде при наведении', async() => {
        await render(<DeveloperCardRewards developer={developerWithRewards} />,
            { viewport: { width: 600, height: 300 } }
        );

        await page.hover('.DeveloperCardRewards__item:nth-child(3)');

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('рисует тултип с информацией о награде и годом при наведении', async() => {
        await render(<DeveloperCardRewards developer={developerWithRewards} />,
            { viewport: { width: 600, height: 300 } }
        );

        await page.hover('.DeveloperCardRewards__item:nth-child(1)');

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });
});
