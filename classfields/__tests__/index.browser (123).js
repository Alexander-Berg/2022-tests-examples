import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'view/react/libs/test-helpers';
import reducer from 'view/react/deskpad/reducers/roots/common';

import { Comparison } from '../';

import {
    emptyStore,
    emptyStoreWithMoreThanOneFilterValue,
    singleCardStore,
    apartmentCardsStore,
    roomCardsStore,
    houseCardsStore,
    lotCardsStore,
    garageCardsStore,
    commercialCardsStore
} from './stub/store';

const Component = props => (
    <AppProvider
        rootReducer={reducer}
        Gate={props.Gate}
        initialState={props.store}
    >
        <Comparison />
    </AppProvider>
);

const VIEWPORT = { width: 800, height: 600 };

describe('Comparison', () => {
    it('заглушка', async() => {
        await render(<Component store={emptyStore} />, { viewport: VIEWPORT });

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('пустой список', async() => {
        await render(<Component store={emptyStoreWithMoreThanOneFilterValue} />, { viewport: VIEWPORT });

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('таблица с одной карточкой', async() => {
        await render(<Component store={singleCardStore} />, { viewport: VIEWPORT });

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('купить апартаменты', async() => {
        await render(<Component store={apartmentCardsStore} />, { viewport: VIEWPORT });

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('купить комнату', async() => {
        await render(<Component store={roomCardsStore} />, { viewport: VIEWPORT });

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('купить дом', async() => {
        await render(<Component store={houseCardsStore} />, { viewport: VIEWPORT });

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('купить участок', async() => {
        await render(<Component store={lotCardsStore} />, { viewport: VIEWPORT });

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('купить гараж', async() => {
        await render(<Component store={garageCardsStore} />, { viewport: VIEWPORT });

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('купить коммерческую недвижимость', async() => {
        await render(<Component store={commercialCardsStore} />, { viewport: VIEWPORT });

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('только отличия', async() => {
        await render(<Component store={commercialCardsStore} />, { viewport: VIEWPORT });

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

        await page.click('[data-test=comparison-only-different]');

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('поделиться', async() => {
        await render(<Component store={garageCardsStore} />, { viewport: VIEWPORT });

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

        await page.click('[data-test=comparison-share]');

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('удаление', async() => {
        const Gate = {
            create: path => {
                // eslint-disable-next-line default-case
                switch (path) {
                    case 'ugc.uncompare': {
                        return Promise.resolve({
                            isDone: true
                        });
                    }
                }
            }
        };

        await render(<Component Gate={Gate} store={garageCardsStore} />, { viewport: VIEWPORT });

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

        await page.click('[data-test=comparison-delete-card]:nth-child(1)');

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });
});
