import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import UnauthorizedTile from '../';

describe('EGRNUnauthorizedTile', () => {
    it('should match screenshot', async() => {
        await render(
            <UnauthorizedTile title='title' description='description' />,
            { viewport: { width: 200, height: 100 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
