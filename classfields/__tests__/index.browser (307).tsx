import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { ISiteSnippetType } from 'realty-core/types/siteSnippet';
import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { SiteMapSnippet } from '../';

import { getSiteSnippet, getInitialState } from './mocks';

const getProps = (item: ISiteSnippetType) => ({
    item,
    source: '',
    subscriptionLocation: '',
    searchParams: {},
    link: () => '',
});

describe('SiteMapSnippet', () => {
    it('рисует сниппет', async () => {
        const props = getProps(getSiteSnippet());

        await render(
            <AppProvider initialState={getInitialState()}>
                <SiteMapSnippet {...props} />,
            </AppProvider>,
            { viewport: { width: 320, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует сниппет без метро', async () => {
        const props = getProps(getSiteSnippet({ withoutMetro: true }));

        await render(
            <AppProvider initialState={getInitialState()}>
                <SiteMapSnippet {...props} />,
            </AppProvider>,
            { viewport: { width: 320, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует сниппет (заморожено)', async () => {
        const props = getProps(getSiteSnippet({ suspended: true }));

        await render(
            <AppProvider initialState={getInitialState()}>
                <SiteMapSnippet {...props} />,
            </AppProvider>,
            { viewport: { width: 320, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует сниппет (нет квартир)', async () => {
        const props = getProps(getSiteSnippet({ withoutFlats: true }));

        await render(
            <AppProvider initialState={getInitialState()}>
                <SiteMapSnippet {...props} />,
            </AppProvider>,
            { viewport: { width: 320, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
