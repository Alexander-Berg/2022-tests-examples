/* eslint-disable jest/expect-expect */
import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { SiteCardProgressQuarterPhotos } from '..';

import {
    dataWithOnePhoto,
    dataWithTwoPhotos,
    dataWithThreePhotos,
    dataWithFourPhotos,
    dataWithSixPhotos,
    dataWithSevenPhotos,
    dataWithVerticalPhotos,
} from './mocks';

const viewports = [
    { width: 1000, height: 600 },
    { width: 700, height: 600 },
] as const;

const renderMultiple = async (component: React.ReactElement) => {
    for (const viewport of viewports) {
        await render(component, { viewport });

        expect(
            await takeScreenshot({
                fullPage: true,
            })
        ).toMatchImageSnapshot();
    }
};

describe('SiteCardProgressQuarterPhotos', () => {
    it('Корректно рендерит блок с 1 фото', async () => {
        await renderMultiple(
            <AppProvider>
                <SiteCardProgressQuarterPhotos quarterData={dataWithOnePhoto} locativeSiteName="в ЖК Центральный" />
            </AppProvider>
        );
    });

    it('Корректно рендерит блок с 2 фото', async () => {
        await renderMultiple(
            <AppProvider>
                <SiteCardProgressQuarterPhotos quarterData={dataWithTwoPhotos} locativeSiteName="в ЖК Центральный" />
            </AppProvider>
        );
    });

    it('Корректно рендерит блок с 3 фото', async () => {
        await renderMultiple(
            <AppProvider>
                <SiteCardProgressQuarterPhotos quarterData={dataWithThreePhotos} locativeSiteName="в ЖК Центральный" />
            </AppProvider>
        );
    });

    it('Корректно рендерит блок с 4 фото (+ плашка показать больше)', async () => {
        await renderMultiple(
            <AppProvider>
                <SiteCardProgressQuarterPhotos quarterData={dataWithFourPhotos} locativeSiteName="в ЖК Центральный" />
            </AppProvider>
        );
    });

    it('Корректно рендерит блок с 6 фото', async () => {
        await renderMultiple(
            <AppProvider>
                <SiteCardProgressQuarterPhotos quarterData={dataWithSixPhotos} locativeSiteName="в ЖК Центральный" />
            </AppProvider>
        );
    });

    it('Корректно рендерит блок с 7 фото (+ плашка показать больше)', async () => {
        await renderMultiple(
            <AppProvider>
                <SiteCardProgressQuarterPhotos quarterData={dataWithSevenPhotos} locativeSiteName="в ЖК Центральный" />
            </AppProvider>
        );
    });

    it('Корректно рендерит блок с вертикальными фото', async () => {
        await renderMultiple(
            <AppProvider>
                <SiteCardProgressQuarterPhotos
                    quarterData={dataWithVerticalPhotos}
                    locativeSiteName="в ЖК Центральный"
                />
            </AppProvider>
        );
    });
});
