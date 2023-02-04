import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { IGeoStore } from 'realty-core/view/react/common/reducers/geo';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { SearchNotFoundByGeo } from '../';

import { getGeoWithDifferentPopulatedRgid, getGeoWithSamePopulatedRgid } from './mocks';

describe('SearchNotFoundByGeo', () => {
    describe('рендерится корректно', () => {
        it('в случае если есть куда расширятся по гео', async () => {
            const geo = getGeoWithDifferentPopulatedRgid() as IGeoStore;
            await render(
                <AppProvider>
                    <SearchNotFoundByGeo geo={geo} pageType="sites-search" params={{}} />
                </AppProvider>,
                { viewport: { width: 375, height: 400 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
        it('в случае если некуда расширятся по гео', async () => {
            const geo = getGeoWithSamePopulatedRgid() as IGeoStore;
            await render(
                <AppProvider>
                    <SearchNotFoundByGeo geo={geo} pageType="sites-search" params={{}} />
                </AppProvider>,
                { viewport: { width: 375, height: 400 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });
});
