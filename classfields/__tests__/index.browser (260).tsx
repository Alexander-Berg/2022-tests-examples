import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';
import { IOfferSnippet } from 'realty-core/types/offerSnippet';

import { OfferMapSnippetContainer } from '../container';

import { getOfferSnippet, getInitialState } from './mocks';

const getProps = (item: IOfferSnippet) => ({
    item,
    source: '',
    page: 'offers-search-map',
    pageType: 'offers-search-map',
    eventPlace: 'map',
    searchParams: {},
    pageSize: 1,
    placement: 'map',
    position: 0,
});

describe('OfferMapSnippet', () => {
    it('рисует сниппет', async () => {
        const props = getProps(getOfferSnippet());

        await render(
            <AppProvider initialState={getInitialState()}>
                <OfferMapSnippetContainer {...props} />,
            </AppProvider>,
            { viewport: { width: 360, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует сниппет без метро', async () => {
        const props = getProps(getOfferSnippet({ withoutMetro: true }));

        await render(
            <AppProvider initialState={getInitialState()}>
                <OfferMapSnippetContainer {...props} />,
            </AppProvider>,
            { viewport: { width: 320, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует сниппет с чатами', async () => {
        const props = getProps(getOfferSnippet({ withChats: true }));

        await render(
            <AppProvider initialState={getInitialState()}>
                <OfferMapSnippetContainer {...props} />,
            </AppProvider>,
            { viewport: { width: 320, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
