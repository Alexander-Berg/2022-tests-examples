import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';
import { ApartmentType, ConstructionState } from 'realty-core/types/siteCard';

import { NewbuildingCardApplication } from '../';

import { siteCard } from './mocks';

const context = {
    link: (): string => 'link',
};

describe('NewbuildingCardApplication', () => {
    it('Рисует экран заяки на новые квартиры', async () => {
        await render(
            <AppProvider initialState={{}} context={context}>
                <NewbuildingCardApplication card={siteCard} />
            </AppProvider>,
            {
                viewport: { width: 360, height: 500 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует экран заяки на новые апартаменты', async () => {
        await render(
            <AppProvider initialState={{}} context={context}>
                <NewbuildingCardApplication
                    card={{
                        ...siteCard,
                        buildingFeatures: {
                            isApartment: true,
                            apartmentType: ApartmentType.APARTMENTS,
                            state: ConstructionState.UNFINISHED,
                        },
                    }}
                />
            </AppProvider>,
            {
                viewport: { width: 360, height: 500 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует экран заяки на новые квартиры и апартаменты', async () => {
        await render(
            <AppProvider initialState={{}} context={context}>
                <NewbuildingCardApplication
                    card={{
                        ...siteCard,
                        buildingFeatures: {
                            isApartment: true,
                            apartmentType: ApartmentType.APARTMENTS_AND_FLATS,
                            state: ConstructionState.UNFINISHED,
                        },
                    }}
                />
            </AppProvider>,
            {
                viewport: { width: 360, height: 500 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Открывает модальное окно при клике на кнопку', async () => {
        await render(
            <AppProvider initialState={{}} context={context}>
                <NewbuildingCardApplication card={siteCard} />
            </AppProvider>,
            {
                viewport: { width: 360, height: 500 },
            }
        );

        await page.click('.Button');

        await page.waitForSelector('.Modal_visible');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
