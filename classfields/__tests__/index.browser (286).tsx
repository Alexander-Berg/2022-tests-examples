/* eslint-disable jest/expect-expect */
import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import RefinementLabel from '..';

import { highwayRefinement, mskRgid, railwayRefinement, highwayRefinementWithoutName, spbRgid } from './mocks';

describe('RefinementLabel', () => {
    it('рисует расстояние до кад', async () => {
        await render(<RefinementLabel subjectFederationRgid={spbRgid} refinement={highwayRefinement} />, {
            viewport: { width: 320, height: 100 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует расстояние до мкад', async () => {
        await render(<RefinementLabel subjectFederationRgid={mskRgid} refinement={highwayRefinement} />, {
            viewport: { width: 320, height: 100 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует расстояние до жд станции', async () => {
        await render(<RefinementLabel subjectFederationRgid={mskRgid} refinement={railwayRefinement} />, {
            viewport: { width: 320, height: 100 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует расстояние до кад без названия', async () => {
        await render(<RefinementLabel subjectFederationRgid={mskRgid} refinement={highwayRefinementWithoutName} />, {
            viewport: { width: 320, height: 100 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
