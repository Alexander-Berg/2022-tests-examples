import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { EGRNAddressPurchasePromoText } from '../';

describe('EGRNAddressPurchasePromoText', () => {
    it('рендерится', async () => {
        await render(<EGRNAddressPurchasePromoText />, {
            viewport: { width: 1280, height: 1550 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
