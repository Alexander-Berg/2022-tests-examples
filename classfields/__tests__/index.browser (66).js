import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AlfabankPromoBanner3 } from '../';

describe('AlfabankPromoBanner3', () => {
    it('рисует промо баннер №3', async() => {
        await render(
            <AlfabankPromoBanner3 />,
            { viewport: { width: 320, height: 450 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
