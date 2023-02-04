import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { ItemOpenChat } from '../index';

import { baseProps } from './mocks';

describe('ItemOpenChat', () => {
    it('Базовая отрисовка', async () => {
        await render(<ItemOpenChat {...baseProps} />, {
            viewport: { width: 100, height: 100 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('При наведении должна показываться подсказка, если переданы withCustomHint и title', async () => {
        await render(<ItemOpenChat {...baseProps} withCustomHint title="подсказка" />, {
            viewport: { width: 200, height: 200 },
        });
        await page.hover('button');
        await page.waitFor(200);

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });
});
