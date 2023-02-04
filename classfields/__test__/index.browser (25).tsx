import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';
import { ApartmentType } from 'realty-core/types/siteCard';

import OfferCardSiteFlats from '../index';

import { getItems, getSite, linkMock } from './mocks';

describe('OfferCardSiteFlats', () => {
    it('базовая отрисовка (квартиры)', async () => {
        await render(
            <AppProvider>
                <OfferCardSiteFlats
                    item={getItems({})}
                    site={getSite({ apartmentType: ApartmentType.FLATS })}
                    link={linkMock}
                />
            </AppProvider>,
            { viewport: { width: 360, height: 200 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('базовая отрисовка (апартаменты)', async () => {
        await render(
            <AppProvider>
                <OfferCardSiteFlats
                    item={getItems({})}
                    site={getSite({ apartmentType: ApartmentType.APARTMENTS })}
                    link={linkMock}
                />
            </AppProvider>,
            { viewport: { width: 360, height: 200 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('базовая отрисовка (квартиры и апартаменты)', async () => {
        await render(
            <AppProvider>
                <OfferCardSiteFlats
                    item={getItems({})}
                    site={getSite({ apartmentType: ApartmentType.APARTMENTS_AND_FLATS })}
                    link={linkMock}
                />
            </AppProvider>,
            { viewport: { width: 360, height: 200 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
