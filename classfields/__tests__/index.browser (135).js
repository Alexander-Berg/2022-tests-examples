import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { OfferPreviewSnippet } from '../';
import styles from '../styles.module.css';

import { getInitialState, getOffer, getEmptyOffer } from './mocks';

const Component = ({ initialState, item }) => (
    <AppProvider initialState={initialState}>
        <OfferPreviewSnippet item={item} />
    </AppProvider>
);

describe('OfferPreviewSnippet', () => {
    it('Рисует сниппет', async() => {
        await render(
            <Component initialState={getInitialState()} item={getOffer()} />,
            { viewport: { width: 360, height: 350 } }
        );
        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует сниппет с ховером', async() => {
        await render(
            <Component initialState={getInitialState()} item={getOffer()} />,
            { viewport: { width: 360, height: 350 } }
        );

        await page.hover(`.${styles.snippet}`);

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('Рисует сниппет без избранного метро и этажей', async() => {
        const initialState = getInitialState();

        initialState.user.favorites = [];
        initialState.user.favoritesMap = {};

        await render(
            <Component initialState={initialState} item={getEmptyOffer()} />,
            { viewport: { width: 360, height: 350 } }
        );
        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
