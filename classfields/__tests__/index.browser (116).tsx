import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { OfferEGRNBlockedFeatureTile } from '../index';

const OPTIONS = { viewport: { width: 200, height: 100 } };

describe('BlockedFeatureTile', () => {
    it('should render', async () => {
        await render(<OfferEGRNBlockedFeatureTile>detailed info</OfferEGRNBlockedFeatureTile>, OPTIONS);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render with long text (2 lines)', async () => {
        await render(<OfferEGRNBlockedFeatureTile>detailed info detailed info</OfferEGRNBlockedFeatureTile>, OPTIONS);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
