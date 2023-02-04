import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers.js';

import SiteCardInfoNew from '..';
import styles from '../styles.module.css';

import { getSiteCard, getSiteCard2, getSalesDepartmentMock } from './mocks';

// eslint-disable-next-line no-undef
global.BUNDLE_LANG = 'ru';

const emptyInitialState = {
    user: {
        favoritesMap: {}
    },
    page: {
        name: 'newbuilding'
    },
    cardPhones: {},
    backCall: {},
    geo: {}
};

const phoneStats = {
    goal: { show: [] },
    gate: { show: '' }
};

describe('SiteCardInfoNew', () => {
    it('рисует блок контактов', async() => {
        await render(
            <AppProvider initialState={emptyInitialState}>
                <SiteCardInfoNew
                    card={getSiteCard()}
                    phoneStats={phoneStats}
                />
            </AppProvider>,
            { viewport: { width: 430, height: 600 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует блок контактов с hover на избранном', async() => {
        await render(
            <AppProvider initialState={emptyInitialState}>
                <SiteCardInfoNew
                    card={getSiteCard()}
                    phoneStats={phoneStats}
                />
            </AppProvider>,
            { viewport: { width: 430, height: 600 } }
        );

        await page.hover(`.${styles.favorite}`);

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('рисует блок контактов, объект добавлен в избранное', async() => {
        const initialState = {
            ...emptyInitialState,
            user: {
                favoritesMap: {
                    site_375274: true
                },
                favorites: [ 'site_375274' ]
            }
        };

        await render(
            <AppProvider initialState={initialState}>
                <SiteCardInfoNew
                    card={getSiteCard()}
                    phoneStats={phoneStats}
                />
            </AppProvider>,
            { viewport: { width: 430, height: 600 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует блок контактов, раскрыт весь список метро', async() => {
        await render(
            <AppProvider initialState={emptyInitialState}>
                <SiteCardInfoNew
                    card={getSiteCard()}
                    phoneStats={phoneStats}
                />
            </AppProvider>,
            { viewport: { width: 430, height: 700 } }
        );

        await page.click('[class^=CardMetroList] .Link');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует блок контактов с hover на показать еще', async() => {
        await render(
            <AppProvider initialState={emptyInitialState}>
                <SiteCardInfoNew
                    card={getSiteCard()}
                    phoneStats={phoneStats}
                />
            </AppProvider>,
            { viewport: { width: 430, height: 600 } }
        );

        await page.hover('[class^=CardMetroList] .Link');

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('рисует блок контактов с hover на ссылку застройщика', async() => {
        await render(
            <AppProvider initialState={emptyInitialState}>
                <SiteCardInfoNew
                    card={getSiteCard()}
                    phoneStats={phoneStats}
                />
            </AppProvider>,
            { viewport: { width: 430, height: 600 } }
        );

        await page.hover('[class^=CardDevBadgeNew] .Link');

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    describe('темная тема', () => {
        it('рисует блок контактов', async() => {
            await render(
                <AppProvider initialState={emptyInitialState}>
                    <SiteCardInfoNew
                        card={getSiteCard()}
                        phoneStats={phoneStats}
                        view='dark'
                    />
                </AppProvider>,
                { viewport: { width: 430, height: 600 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('рисует блок контактов с hover на избранном', async() => {
            await render(
                <AppProvider initialState={emptyInitialState}>
                    <SiteCardInfoNew
                        card={getSiteCard()}
                        phoneStats={phoneStats}
                        view='dark'
                    />
                </AppProvider>,
                { viewport: { width: 430, height: 600 } }
            );

            await page.hover(`.${styles.favorite}`);

            expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
        });

        it('рисует блок контактов, объект добавлен в избранное', async() => {
            const initialState = {
                ...emptyInitialState,
                user: {
                    favoritesMap: {
                        site_375274: true
                    },
                    favorites: [ 'site_375274' ]
                }
            };

            await render(
                <AppProvider initialState={initialState}>
                    <SiteCardInfoNew
                        card={getSiteCard()}
                        phoneStats={phoneStats}
                        view='dark'
                    />
                </AppProvider>,
                { viewport: { width: 430, height: 600 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('рисует блок контактов, раскрыт весь список метро', async() => {
            await render(
                <AppProvider initialState={emptyInitialState}>
                    <SiteCardInfoNew
                        card={getSiteCard()}
                        phoneStats={phoneStats}
                        view='dark'
                    />
                </AppProvider>,
                { viewport: { width: 430, height: 600 } }
            );

            await page.click('[class^=CardMetroList] .Link');

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('рисует блок контактов с hover на показать еще', async() => {
            await render(
                <AppProvider initialState={emptyInitialState}>
                    <SiteCardInfoNew
                        card={getSiteCard()}
                        phoneStats={phoneStats}
                        view='dark'
                    />
                </AppProvider>,
                { viewport: { width: 430, height: 600 } }
            );

            await page.hover('[class^=CardMetroList] .Link');

            expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
        });

        it('рисует блок контактов с hover на ссылку застройщика', async() => {
            await render(
                <AppProvider initialState={emptyInitialState}>
                    <SiteCardInfoNew
                        card={getSiteCard()}
                        phoneStats={phoneStats}
                        view='dark'
                    />
                </AppProvider>,
                { viewport: { width: 430, height: 600 } }
            );

            await page.hover('[class^=CardDevBadgeNew] .Link');

            expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
        });

        it('рисует блок контактов с несколькими застройщиками', async() => {
            await render(
                <AppProvider initialState={emptyInitialState}>
                    <SiteCardInfoNew
                        card={getSiteCard2()}
                        phoneStats={phoneStats}
                        view='dark'
                    />
                </AppProvider>,
                { viewport: { width: 430, height: 600 } }
            );

            expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
        });

        it('рисует блок контактов с открытым телефоном', async() => {
            const card = getSiteCard2();

            await render(
                <AppProvider
                    initialState={{
                        ...emptyInitialState,
                        cardPhones: {
                            KzcF0OHTUJzNLjMN0OPTkR4: '+74951062033'
                        },
                        salesDepartment: {
                            ...getSalesDepartmentMock()
                        }
                    }}
                >
                    <SiteCardInfoNew
                        card={card}
                        redirectParams={{
                            objectId: card.id,
                            objectType: 'newbuilding'
                        }}
                        phoneStats={phoneStats}
                        view='dark'
                    />
                </AppProvider>,
                { viewport: { width: 430, height: 600 } }
            );

            expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
        });

        it('рисует блок контактов после клика на список застройщиков', async() => {
            await render(
                <AppProvider initialState={emptyInitialState}>
                    <SiteCardInfoNew
                        card={getSiteCard2()}
                        phoneStats={phoneStats}
                        view='dark'
                    />
                </AppProvider>,
                { viewport: { width: 500, height: 600 } }
            );

            await page.click('[data-test="developers-more"]');

            expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
        });
    });
});
