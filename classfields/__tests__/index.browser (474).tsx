import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';
import { generateImageUrl } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';
import { ISiteSnippetType } from 'realty-core/types/siteSnippet';

import { SerpRelatedNewbuildingsSnippet } from '../';

const getMock = (fields?: Partial<ISiteSnippetType>) =>
    (({
        images: [generateImageUrl({ width: 200, height: 150 })],
        name: 'Тёплый Берег',
        filteredPrimaryOfferStats: {
            primaryOffers: 501,
            priceRange: {
                from: 4000000,
                to: 12000000,
            },
        },
        location: {
            settlementRgid: 165705,
        },
        ...fields,
    } as unknown) as ISiteSnippetType);

const Component = ({ siteSnippet }: { siteSnippet: ISiteSnippetType }) => (
    <AppProvider>
        <SerpRelatedNewbuildingsSnippet siteSnippet={siteSnippet} />
    </AppProvider>
);

describe('SerpRelatedNewbuildingSnippet', () => {
    it('рендерится в дефолтном состоянии', async () => {
        await render(<Component siteSnippet={getMock()} />, { viewport: { width: 300, height: 350 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится в дефолтном состоянии, ховер по карточке', async () => {
        await render(<Component siteSnippet={getMock()} />, { viewport: { width: 300, height: 350 } });

        await page.hover('.Link');

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('рендерится без картинки, без цены и без количества квартир', async () => {
        await render(
            <Component siteSnippet={getMock({ name: undefined, images: [], filteredPrimaryOfferStats: undefined })} />,
            {
                viewport: { width: 300, height: 350 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится с длинной ценой, длинным названием и длинным количеством квартир', async () => {
        await render(
            <Component
                siteSnippet={getMock({
                    name: 'Длинное название ЖК длинное название ЖК длинное название ЖК длинное название ЖК',
                    filteredPrimaryOfferStats: {
                        primaryOffers: 999999,
                        price: { from: Number.MAX_SAFE_INTEGER, to: 123 },
                    },
                })}
            />,
            { viewport: { width: 300, height: 350 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
