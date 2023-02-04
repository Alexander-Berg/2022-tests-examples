import React from 'react';
import { render } from 'jest-puppeteer-react';
import noop from 'lodash/noop';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { OfferSerpStrikeoutPlaceholder } from '../';

describe('OfferSerpScrikeoutPlaceholder', () => {
    it('рендерится корректно', async () => {
        await render(<OfferSerpStrikeoutPlaceholder offerId="" onClick={noop} />, {
            viewport: { width: 320, height: 100 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
