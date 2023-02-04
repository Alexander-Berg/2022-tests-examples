import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { OfferCardAddressWithSiteLink } from '../index';

import {
    offerWithoutStructuredAddress,
    offerWithCity,
    offerWithDistrict,
    offerWithCityAndDistrict,
    site,
} from './mocks';

describe('OfferCardLocation', function () {
    it('Отрисовка когда нет ни города, ни округа', async () => {
        await render(
            <AppProvider>
                <OfferCardAddressWithSiteLink offer={offerWithoutStructuredAddress} site={site} />
            </AppProvider>,
            {
                viewport: { width: 400, height: 150 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка когда есть только город', async () => {
        await render(
            <AppProvider>
                <OfferCardAddressWithSiteLink offer={offerWithCity} site={site} />
            </AppProvider>,
            {
                viewport: { width: 400, height: 150 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка когда есть только округ', async () => {
        await render(
            <AppProvider>
                <OfferCardAddressWithSiteLink offer={offerWithDistrict} site={site} />
            </AppProvider>,
            {
                viewport: { width: 400, height: 150 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка когда есть город и округ', async () => {
        await render(
            <AppProvider>
                <OfferCardAddressWithSiteLink offer={offerWithCityAndDistrict} site={site} />
            </AppProvider>,
            {
                viewport: { width: 400, height: 150 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
