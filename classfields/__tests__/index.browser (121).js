import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers.js';

import SiteCardFastLinks from '..';
import styles from '../styles.module.css';

import { getLinks } from './mocks';

const emptyInitialState = {
    user: {
        favoritesMap: {}
    },
    cardPhones: {},
    backCall: {}
};

describe('SiteCardFastLinks', () => {
    it('рисует ссылки, 1 тип отделки', async() => {
        await render(
            <AppProvider initialState={emptyInitialState}>
                <SiteCardFastLinks links={getLinks()} />
            </AppProvider>,
            { viewport: { width: 700, height: 100 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует ссылки, hover на первую ссылку, 3 типа отделка', async() => {
        await render(
            <AppProvider initialState={emptyInitialState}>
                <SiteCardFastLinks links={getLinks({ count: 3 })} />
            </AppProvider>,
            { viewport: { width: 700, height: 100 } }
        );

        await page.hover(`.${styles.link}`);

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('рисует ссылки, hover на первую ссылку, 10 типов отделка', async() => {
        await render(
            <AppProvider initialState={emptyInitialState}>
                <SiteCardFastLinks links={getLinks({ count: 10 })} />
            </AppProvider>,
            { viewport: { width: 700, height: 100 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
