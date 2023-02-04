import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AlfabankPromoBanner1 } from '../';

describe('AlfabankPromoBanner1', () => {
    it('рисует промо баннер №1', async() => {
        await render(
            <AlfabankPromoBanner1 />,
            { viewport: { width: 940, height: 400 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
