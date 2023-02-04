import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import SitesSerpNotFoundInRegion from '..';

import { getGeoWithDifferentPopulatedRgid, getGeoWithSamePopulatedRgid } from './mocks';

describe('SitesSerpNotFoundInRegion', () => {
    describe('рендерится корректно', () => {
        it('в случае если есть куда расширятся по гео', async() => {
            const geo = getGeoWithDifferentPopulatedRgid();

            await render(
                <AppProvider>
                    <SitesSerpNotFoundInRegion geo={geo} />
                </AppProvider>,
                { viewport: { width: 900, height: 400 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
        it('в случае если некуда расширятся по гео', async() => {
            const geo = getGeoWithSamePopulatedRgid();

            await render(
                <AppProvider>
                    <SitesSerpNotFoundInRegion geo={geo} />
                </AppProvider>,
                { viewport: { width: 900, height: 400 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });
});
