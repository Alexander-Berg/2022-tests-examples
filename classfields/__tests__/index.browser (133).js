import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'view/react/libs/test-helpers';

import { OfferMapSnippet } from '../';

import {
    getInitialState,
    getInitialStateWithNotes,
    getOfferWithVas,
    getRentOfferWithVas,
    getCommercialOffer,
    getOfferWithBadges,
    getSiteOffer
} from './mocks';

// eslint-disable-next-line no-undef
global.BUNDLE_LANG = 'ru';

const getProviderProps = initialState => ({
    initialState,
    context: {
        observeIntersection: () => {}
    }
});

describe('OfferMapSnippet', () => {
    it('Рисует заполненный оффер (нет васов и заметок)', async() => {
        const initialState = getInitialState();

        await render(
            <AppProvider {...getProviderProps(initialState)}>
                <OfferMapSnippet
                    item={getOfferWithVas()}
                />,
            </AppProvider>,
            { viewport: { width: 400, height: 520 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует заполненный оффер', async() => {
        const initialState = getInitialStateWithNotes();

        await render(
            <AppProvider {...getProviderProps(initialState)}>
                <OfferMapSnippet
                    item={getOfferWithVas()}
                    isJuridical
                    isAuth
                />,
            </AppProvider>,
            { viewport: { width: 400, height: 600 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует заполненный оффер аренды со всеми васами', async() => {
        const initialState = getInitialStateWithNotes();

        await render(
            <AppProvider {...getProviderProps(initialState)}>
                <OfferMapSnippet
                    item={getRentOfferWithVas()}
                    isJuridical
                    isAuth
                />,
            </AppProvider>,
            { viewport: { width: 400, height: 520 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует оффер без адреса, который добавлен в избранное и сравнение', async() => {
        const initialState = getInitialStateWithNotes();

        await render(
            <AppProvider {...getProviderProps(initialState)}>
                <OfferMapSnippet
                    item={getRentOfferWithVas()}
                    withoutAddress
                    isFavorite
                    isComparing
                />,
            </AppProvider>,
            { viewport: { width: 400, height: 520 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует коммерческий оффер с классом БЦ и длинной ссылкой', async() => {
        const initialState = getInitialState();

        await render(
            <AppProvider {...getProviderProps(initialState)}>
                <OfferMapSnippet
                    item={getCommercialOffer()}
                />,
            </AppProvider>,
            { viewport: { width: 400, height: 520 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует оффер с разными баджами, отчёт росреестра готов', async() => {
        const initialState = getInitialState();

        await render(
            <AppProvider {...getProviderProps(initialState)}>
                <OfferMapSnippet
                    item={getOfferWithBadges('FRA_READY')}
                />,
            </AppProvider>,
            { viewport: { width: 400, height: 520 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует оффер с разными баджами, отчёт росреестра в процессе', async() => {
        const initialState = getInitialState();

        await render(
            <AppProvider {...getProviderProps(initialState)}>
                <OfferMapSnippet
                    item={getOfferWithBadges('FRA_IN_PROGRESS')}
                />,
            </AppProvider>,
            { viewport: { width: 400, height: 520 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует новостроечный оффер c ипотекой (primarySaleV2)', async() => {
        const initialState = getInitialState();

        await render(
            <AppProvider {...getProviderProps(initialState)}>
                <OfferMapSnippet
                    item={getSiteOffer()}

                />,
            </AppProvider>,
            { viewport: { width: 400, height: 520 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
