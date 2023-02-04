import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { MortgageWarning } from '../';

describe('MortgageWarning', () => {
    it('рисуется (белая)', async () => {
        await render(<MortgageWarning />, {
            viewport: { width: 1100, height: 100 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисуется (синяя)', async () => {
        await render(<MortgageWarning view="blue" />, {
            viewport: { width: 1100, height: 100 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
