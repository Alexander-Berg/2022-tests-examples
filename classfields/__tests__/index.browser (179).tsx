import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider, createRootReducer } from 'realty-core/view/react/libs/test-helpers';
import { FavoritesTypes } from 'realty-core/view/react/common/actions/user';

import { CardFavoritesActionContainer } from '../container';

const initialState = {
    user: {
        favorites: [],
        favoritesMap: {},
    },
};

const Gate = {
    create: () =>
        Promise.resolve({
            isDone: true,
        }),
};

const rootReducer = createRootReducer({});

describe('CardFavoritesAction', () => {
    it('Рисует обычную кнопку, если объект не в избранном', async () => {
        await render(
            <AppProvider rootReducer={rootReducer} initialState={initialState} Gate={Gate}>
                <CardFavoritesActionContainer id="111" favoritesType={FavoritesTypes.SITE} />
            </AppProvider>,
            { viewport: { width: 200, height: 200 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует подсвеченную кнопку при добавлении оффера в избранное', async () => {
        await render(
            <AppProvider rootReducer={rootReducer} initialState={initialState} Gate={Gate}>
                <CardFavoritesActionContainer id="111111" favoritesType={FavoritesTypes.OFFER} />
            </AppProvider>,
            { viewport: { width: 200, height: 200 } }
        );

        await page.click('.Button');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует подсвеченную кнопку при добавлении ЖК в избранное', async () => {
        await render(
            <AppProvider rootReducer={rootReducer} initialState={initialState} Gate={Gate}>
                <CardFavoritesActionContainer id="111111" favoritesType={FavoritesTypes.SITE} />
            </AppProvider>,
            { viewport: { width: 200, height: 200 } }
        );

        await page.click('.Button');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует подсвеченную кнопку при добавлении КП в избранное', async () => {
        await render(
            <AppProvider rootReducer={rootReducer} initialState={initialState} Gate={Gate}>
                <CardFavoritesActionContainer id="111111" favoritesType={FavoritesTypes.VILLAGE} />
            </AppProvider>,
            { viewport: { width: 200, height: 200 } }
        );

        await page.click('.Button');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
