import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { OfferCardNearbySites } from '..';

import { site1, site2, site3, site4, site5 } from './mocks';

const context = {
    observeIntersection: (): void => undefined,
    unObserveIntersection: (): void => undefined,
};

function click(selector: string) {
    return page.click(selector);
}

describe('SiteCardSimilarPaid', () => {
    it('рисует 3 ЖК', async () => {
        await render(
            <AppProvider initialState={{}} context={context}>
                <OfferCardNearbySites items={[site1, site2, site3]} />
            </AppProvider>,
            { viewport: { width: 800, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует 5 ЖК', async () => {
        await render(
            <AppProvider initialState={{}} context={context}>
                <OfferCardNearbySites items={[site1, site2, site3, site4, site5]} />
            </AppProvider>,
            { viewport: { width: 800, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует снипеты после переключения вправо', async () => {
        await render(
            <AppProvider initialState={{}} context={context}>
                <OfferCardNearbySites items={[site1, site2, site3, site4, site5]} />
            </AppProvider>,
            { viewport: { width: 800, height: 500 } }
        );

        await click('.SwipeableBlock__forward');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует снипеты в крайнем правом положении', async () => {
        await render(
            <AppProvider initialState={{}} context={context}>
                <OfferCardNearbySites items={[site1, site2, site3, site4, site5]} />
            </AppProvider>,
            { viewport: { width: 800, height: 500 } }
        );

        await click('.SwipeableBlock__forward');
        await click('.SwipeableBlock__forward');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует снипеты после переключения влево', async () => {
        await render(
            <AppProvider initialState={{}} context={context}>
                <OfferCardNearbySites items={[site1, site2, site3, site4, site5]} />
            </AppProvider>,
            { viewport: { width: 800, height: 500 } }
        );

        await click('.SwipeableBlock__forward');
        await click('.SwipeableBlock__forward');
        await click('.SwipeableBlock__back');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
