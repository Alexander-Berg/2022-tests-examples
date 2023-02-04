import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { WalletPaymentAmountModal } from '../index';

const [ WIDTH, HEIGHT ] = [ 800, 600 ];

describe('WalletPaymentAmountModal', () => {
    it('correct draw component', async() => {
        await render(<WalletPaymentAmountModal handleContinue={() => {}} />, {
            viewport: { width: WIDTH, height: HEIGHT }
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});

