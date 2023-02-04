import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers.js';

import SiteCardHeader from '../';

import { getSiteCard, getSiteCard2, getSalesDepartmentMock } from './mocks';

// eslint-disable-next-line @typescript-eslint/ban-ts-comment
// @ts-ignore
global.BUNDLE_LANG = 'ru';

const emptyInitialState = {
    user: {
        favoritesMap: {},
    },
    page: {
        name: 'newbuilding',
    },
    cardPhones: {},
    backCall: {},
    geo: {},
};

const phoneStats = {
    goal: { show: [] },
    gate: { show: '' },
};

describe('SiteCardHeader', () => {
    describe('темная тема', () => {
        it('рисует хедер', async () => {
            await render(
                <AppProvider initialState={emptyInitialState} context={{}}>
                    <SiteCardHeader card={getSiteCard()} phoneStats={phoneStats} view="dark" alwaysShown isMenuHidden />
                </AppProvider>,
                { viewport: { width: 900, height: 250 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('рисует блок контактов с несколькими застройщиками', async () => {
            await render(
                <AppProvider initialState={emptyInitialState} context={{}}>
                    <SiteCardHeader
                        card={getSiteCard2()}
                        phoneStats={phoneStats}
                        view="dark"
                        alwaysShown
                        isMenuHidden
                    />
                </AppProvider>,
                { viewport: { width: 900, height: 250 } }
            );

            expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
        });

        it('рисует блок контактов с открытым телефоном', async () => {
            const card = getSiteCard2();

            await render(
                <AppProvider
                    initialState={{
                        ...emptyInitialState,
                        cardPhones: {
                            KzcF0OHTUJzNLjMN0OPTkR4: '+74951062033',
                        },
                        salesDepartment: {
                            ...getSalesDepartmentMock(),
                        },
                    }}
                    context={{}}
                >
                    <SiteCardHeader
                        card={card}
                        redirectParams={{
                            objectId: card.id,
                            objectType: 'newbuilding',
                        }}
                        phoneStats={phoneStats}
                        view="dark"
                        alwaysShown
                        isMenuHidden
                    />
                </AppProvider>,
                { viewport: { width: 900, height: 250 } }
            );

            expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
        });

        it('рисует блок контактов после клика на список застройщиков', async () => {
            await render(
                <AppProvider initialState={emptyInitialState} context={{}}>
                    <SiteCardHeader
                        card={getSiteCard2()}
                        phoneStats={phoneStats}
                        view="dark"
                        alwaysShown
                        isMenuHidden
                    />
                </AppProvider>,
                { viewport: { width: 900, height: 450 } }
            );

            await page.click('[data-test="developers-more"]');

            expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
        });
    });
});
