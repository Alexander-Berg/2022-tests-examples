import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AlfabankPromoBanner2 } from '../';

describe('AlfabankPromoBanner2', () => {
    it('рисует промо баннер №2', async() => {
        await render(
            <AlfabankPromoBanner2 />,
            { viewport: { width: 320, height: 450 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
