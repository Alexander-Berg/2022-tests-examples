import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { IOfferCard } from 'realty-core/types/offerCard';
import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { ISiteCard } from 'realty-core/types/siteCard';

import { OfferCardBuildingFeaturesContainer } from '../container';

import { newbuildingOffer, secondaryOffer, site, siteWithoutFloorsGap, state } from './mocks';

const Component: React.FC<{ offer: IOfferCard; site?: ISiteCard }> = ({ offer, site }) => (
    <AppProvider initialState={state}>
        <OfferCardBuildingFeaturesContainer offer={offer} site={site} />
    </AppProvider>
);

describe('OfferCardBuildingFeaturesContainer', () => {
    it('Отрисовка с 3-мя удобствами', async () => {
        await render(<Component offer={newbuildingOffer} />, {
            viewport: { width: 800, height: 400 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка со многими удобствами', async () => {
        await render(<Component offer={secondaryOffer} />, {
            viewport: { width: 800, height: 400 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('После клика на "ещё удобства" должны показаться все удобства', async () => {
        await render(<Component offer={secondaryOffer} />, {
            viewport: { width: 800, height: 400 },
        });

        await page.click('.Link_size_unset');
        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка компонента для подписки на объявления в доме', async () => {
        await render(
            <AppProvider initialState={{ ...state, offerCard: { buildingSubscriptionParams: {} } }}>
                <OfferCardBuildingFeaturesContainer offer={secondaryOffer} site={undefined} />
            </AppProvider>,
            { viewport: { width: 800, height: 400 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Если количество этажей в карточке ЖК меньше чем в оффере - не выводим количество этажей', async () => {
        await render(<Component offer={newbuildingOffer} site={site} />, {
            viewport: { width: 800, height: 400 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Если минимальное количество этажей в ЖК больше чем в оффере - не выводим количество этажей', async () => {
        await render(<Component offer={{ ...newbuildingOffer, floorsTotal: 3 }} site={site} />, {
            viewport: { width: 800, height: 400 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Если количество этажей в оффере попадает в диапазон ЖК - выводим количество этажей', async () => {
        await render(<Component offer={{ ...newbuildingOffer, floorsTotal: 13 }} site={site} />, {
            viewport: { width: 800, height: 400 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Если количество этажей в оффере совпадает с ЖК - выводим количество этажей', async () => {
        await render(<Component offer={{ ...newbuildingOffer, floorsTotal: 24 }} site={siteWithoutFloorsGap} />, {
            viewport: { width: 800, height: 400 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
