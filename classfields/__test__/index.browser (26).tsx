import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { SitesSerpSpecialPinnedBlock } from '..';

import { siteSnippetMock } from './mocks';

const samoletSpecialDeveloperData = {
    developerId: 102320,
    developerName: 'Самолёт',
    geoIds: [1, 10174],
};

describe('SitesSerpSpecialPinnedBlock', () => {
    it('рендерится корректно', async () => {
        await render(
            <AppProvider initialState={{ user: { favoritesMap: { site_1: true } } }}>
                <SitesSerpSpecialPinnedBlock
                    items={Array(3)
                        .fill(siteSnippetMock)
                        .map((s, i) => ({ ...s, id: i }))}
                    developerData={samoletSpecialDeveloperData}
                    rgid={741965}
                />
            </AppProvider>,
            { viewport: { width: 700, height: 600 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
