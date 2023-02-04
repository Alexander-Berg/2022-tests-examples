import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { ISiteSnippetType } from 'realty-core/types/siteSnippet';

import { SiteSnippetBadges } from '../';

const TestContainer = (props: { item: ISiteSnippetType }) => (
    <div style={{ height: 160, width: 300, display: 'flex', alignItems: 'stretch', backgroundColor: 'grey' }}>
        <SiteSnippetBadges item={props.item} />
    </div>
);

const getMockSite = (overwrite?: Record<string, unknown>) =>
    (({
        siteSpecialProposals: [
            { specialProposalType: 'discount', description: 'акция' },
            { specialProposalType: 'discount', description: 'акция 2, которая не покажется' },
            { specialProposalType: 'discount', description: 'акция 3, которая не покажется' },
            { specialProposalType: 'discount', description: 'акция 4, которая не покажется' },
            { specialProposalType: 'discount', description: 'акция 5, которая не покажется' },
        ],
        state: 'UNKNOWN',
        finishedApartments: true,
        flatStatus: 'SOON_AVAILABLE',
        buildingClass: 'ECONOM',
        ...overwrite,
    } as unknown) as ISiteSnippetType);

const mocks = ([
    {},
    {
        state: 'BUILT',
        finishedApartments: true,
        buildingClass: 'COMFORT',
        siteSpecialProposals: [
            { specialProposalType: 'gift', description: 'длинное описание акции, которое не влезет на всю ширину' },
            { specialProposalType: 'gift', description: 'акция 2, которая не покажется' },
            { specialProposalType: 'gift', description: 'акция 3, которая не покажется' },
        ],
    },
    { state: 'HAND_OVER', buildingClass: 'COMFORT_PLUS' },
    { state: 'UNFINISHED', finishedApartments: true, buildingClass: 'BUSINESS' },
    { state: 'UNFINISHED', finishedApartments: true, buildingClass: 'ELITE' },
    { state: 'CONSTRUCTION_SUSPENDED', buildingClass: 'STANDART' },
    { state: 'CONSTRUCTION_SUSPENDED_has_finished' },
    { state: 'IN_PROJECT' },
].map((item, index) => [index, getMockSite(item)]) as unknown) as ISiteSnippetType[];

describe('SiteSnippetBadges', () => {
    it.each(mocks)('Рендерится с параметрами %s', async (index, item) => {
        await render(<TestContainer item={item} />, { viewport: { width: 360, height: 200 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
