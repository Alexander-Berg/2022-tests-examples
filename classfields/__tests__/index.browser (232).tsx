import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider, createRootReducer } from 'realty-core/view/react/libs/test-helpers';
import { IOfferSnippet } from 'realty-core/types/offerSnippet';
import { RequestStatus } from 'realty-core/types/network';

import { MortgageOffers } from '../';

import { getOffers, initialState } from './mocks';

const Gate = {
    create: (path: string) => {
        if (path === 'ugc.favorite') {
            return Promise.resolve({ isDone: true, count: 2 });
        }

        if (path === 'ugc.unfavorite') {
            return Promise.resolve({ isDone: true, count: 1 });
        }
    },
};

const Component = ({ items = getOffers() }: { items?: IOfferSnippet[] }) => (
    <AppProvider initialState={initialState} rootReducer={createRootReducer({})} Gate={Gate}>
        <MortgageOffers items={items} searchQuery={{}} total={23} status={RequestStatus.LOADED} />
    </AppProvider>
);

describe('MortgageOffers', () => {
    it('Рисует карусель с офферами', async () => {
        await render(<Component items={[...getOffers(), ...getOffers()]} />, { viewport: { width: 360, height: 450 } });
        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует карусель с офферами (горизонтально)', async () => {
        await render(<Component items={[...getOffers(), ...getOffers()]} />, { viewport: { width: 700, height: 450 } });
        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует карусель без кнопки если сниппетов меньше 4', async () => {
        await render(<Component />, { viewport: { width: 360, height: 450 } });
        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Добавление/удаление оффера в избранное', async () => {
        await render(<Component />, { viewport: { width: 360, height: 450 } });

        await page.click('.SwipeableSlider__item:nth-child(1) .SerpFavoriteAction');
        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click('.SwipeableSlider__item:nth-child(1) .SerpFavoriteAction');
        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
