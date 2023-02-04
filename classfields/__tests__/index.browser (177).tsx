import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { BackCallTermsWithModal } from '..';

describe('BackCallTermsWithModal', () => {
    it('базовая отрисовка', async () => {
        await render(<BackCallTermsWithModal />, { viewport: { width: 400, height: 150 } });
        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует модальное окно с соглашением, после клика на ссылку "согласие"', async () => {
        await render(<BackCallTermsWithModal />, { viewport: { width: 400, height: 450 } });

        await page.click('.Link');
        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
