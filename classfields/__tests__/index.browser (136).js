import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'view/react/libs/test-helpers';

import { OfferSnippetNote } from '../';

import {
    getSavedNoteState,
    getEmptyNoteState,
    getNotSavedNoteState,
    getNotChangedNoteState,
    getLoadingNoteState
} from './mocks';

describe('OfferMapSnippet', () => {
    it('Рисует заполненную сохраненную заметку', async() => {
        await render(
            <AppProvider initialState={getSavedNoteState()}>
                <OfferSnippetNote
                    offerId='1'
                />
            </AppProvider>,
            { viewport: { width: 300, height: 100 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует активную заметку (пустую)', async() => {
        await render(
            <AppProvider initialState={getEmptyNoteState()}>
                <OfferSnippetNote
                    offerId='1'
                    isActive
                />
            </AppProvider>,
            { viewport: { width: 300, height: 100 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует активную заметку', async() => {
        await render(
            <AppProvider initialState={getNotSavedNoteState()}>
                <OfferSnippetNote
                    offerId='1'
                    isActive
                />
            </AppProvider>,
            { viewport: { width: 300, height: 100 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует активную заметку (не измененную)', async() => {
        await render(
            <AppProvider initialState={getNotChangedNoteState()}>
                <OfferSnippetNote
                    offerId='1'
                    isActive
                />
            </AppProvider>,
            { viewport: { width: 300, height: 100 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует заметку в состоянии загрузки', async() => {
        await render(
            <AppProvider initialState={getLoadingNoteState()}>
                <OfferSnippetNote
                    offerId='1'
                />
            </AppProvider>,
            { viewport: { width: 300, height: 100 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
