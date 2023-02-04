import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AlfabankPromoFeatures } from '../';

describe('AlfabankPromoFeatures', () => {
    it('рисует блок с основными условиями ипотеки', async() => {
        await render(
            <AlfabankPromoFeatures />,
            { viewport: { width: 320, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
