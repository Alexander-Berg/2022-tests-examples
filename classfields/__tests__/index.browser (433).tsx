import React from 'react';

import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { IOfferCard } from 'realty-core/types/offerCard';

import GeoLinks from '../';

import { offer, offerEmptyMetroList } from './mocks';

describe('GeoLinks', () => {
    it('Должен отрендерить компонент, когда есть данные о метро', async () => {
        await render(
            <AppProvider>
                <GeoLinks offer={offer as IOfferCard} />
            </AppProvider>,
            { viewport: { width: 1000, height: 400 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Должен отрендерить компонент, когда нет данных о метро', async () => {
        await render(
            <AppProvider>
                <GeoLinks offer={offerEmptyMetroList as IOfferCard} />
            </AppProvider>,
            { viewport: { width: 1000, height: 400 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
