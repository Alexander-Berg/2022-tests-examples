import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { MortgageBankCardDescription } from '../';

import { getBank } from './mocks';

describe('MortgageBankCardDescription', () => {
    it('рисует полностью заполненный блок', async () => {
        await render(<MortgageBankCardDescription bank={getBank()} />, {
            viewport: { width: 320, height: 700 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует блок без описания', async () => {
        await render(<MortgageBankCardDescription bank={{ ...getBank(), description: undefined }} />, {
            viewport: { width: 320, height: 400 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
