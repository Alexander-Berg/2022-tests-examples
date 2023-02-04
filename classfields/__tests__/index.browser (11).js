import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { TaxiBannerComponent } from '../';

const MIN_WIDTH_L = 800;
const MAX_WIDTH_L = 1170;
const MIN_WIDTH_S = 328;
const MAX_WIDTH_S = 680;

const GEO_WITH_BANNER = {
    rgid: 741965 // spb
};

const TEST_CASES = [
    { size: 'l', width: MIN_WIDTH_L, bannerIndex: 0, slideIndex: 0 },
    { size: 'l', width: MIN_WIDTH_L, bannerIndex: 0, slideIndex: 1 },
    { size: 'l', width: MIN_WIDTH_L, bannerIndex: 1, slideIndex: 0 },
    { size: 'l', width: MIN_WIDTH_L, bannerIndex: 1, slideIndex: 1 },
    { size: 'l', width: MIN_WIDTH_L, bannerIndex: 2, slideIndex: 0 },
    { size: 'l', width: MIN_WIDTH_L, bannerIndex: 2, slideIndex: 1 },

    { size: 'l', width: MAX_WIDTH_L, bannerIndex: 0, slideIndex: 0 },
    { size: 'l', width: MAX_WIDTH_L, bannerIndex: 0, slideIndex: 1 },
    { size: 'l', width: MAX_WIDTH_L, bannerIndex: 1, slideIndex: 0 },
    { size: 'l', width: MAX_WIDTH_L, bannerIndex: 1, slideIndex: 1 },
    { size: 'l', width: MAX_WIDTH_L, bannerIndex: 2, slideIndex: 0 },
    { size: 'l', width: MAX_WIDTH_L, bannerIndex: 2, slideIndex: 1 },

    { size: 's', width: MIN_WIDTH_S, bannerIndex: 0, slideIndex: 0 },
    { size: 's', width: MIN_WIDTH_S, bannerIndex: 0, slideIndex: 1 },
    { size: 's', width: MIN_WIDTH_S, bannerIndex: 0, slideIndex: 2 },
    { size: 's', width: MIN_WIDTH_S, bannerIndex: 1, slideIndex: 0 },
    { size: 's', width: MIN_WIDTH_S, bannerIndex: 1, slideIndex: 1 },
    { size: 's', width: MIN_WIDTH_S, bannerIndex: 1, slideIndex: 2 },
    { size: 's', width: MIN_WIDTH_S, bannerIndex: 2, slideIndex: 0 },
    { size: 's', width: MIN_WIDTH_S, bannerIndex: 2, slideIndex: 1 },

    { size: 's', width: MAX_WIDTH_S, bannerIndex: 0, slideIndex: 0 },
    { size: 's', width: MAX_WIDTH_S, bannerIndex: 0, slideIndex: 1 },
    { size: 's', width: MAX_WIDTH_S, bannerIndex: 0, slideIndex: 2 },
    { size: 's', width: MAX_WIDTH_S, bannerIndex: 1, slideIndex: 0 },
    { size: 's', width: MAX_WIDTH_S, bannerIndex: 1, slideIndex: 1 },
    { size: 's', width: MAX_WIDTH_S, bannerIndex: 1, slideIndex: 2 },
    { size: 's', width: MAX_WIDTH_S, bannerIndex: 2, slideIndex: 0 },
    { size: 's', width: MAX_WIDTH_S, bannerIndex: 2, slideIndex: 1 }
];

describe('TaxiBanner', () => {
    TEST_CASES.forEach(({ width, ...props }) => (
        it(`should render banner on screen of width ${width} with params ${JSON.stringify(props)}`, async() => {
            await render(
                <TaxiBannerComponent
                    geo={GEO_WITH_BANNER}
                    {...props}
                />,
                { viewport: { width, height: 150 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        })
    ));
});
