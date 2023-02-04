import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { SitePlansSnippet } from '..';

import { getInitialState, getSitePlan } from './mocks';

describe('SitePlansSnippet', () => {
    it('рисует сниппет в состоянии загрузки', async() => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <SitePlansSnippet
                    plan={getSitePlan()}
                    isLoading
                />
            </AppProvider>,
            { viewport: { width: 360, height: 240 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует сниппет без изображения', async() => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <SitePlansSnippet
                    plan={getSitePlan({ withImages: false })}
                />
            </AppProvider>,
            { viewport: { width: 360, height: 240 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует сниппет студии', async() => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <SitePlansSnippet
                    plan={getSitePlan({ isStudio: true })}
                />
            </AppProvider>,
            { viewport: { width: 360, height: 240 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует сниппет с 1 оффером', async() => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <SitePlansSnippet
                    plan={getSitePlan({
                        offersCount: 1
                    })}
                />
            </AppProvider>,
            { viewport: { width: 360, height: 240 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует сниппет с несколькими офферами', async() => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <SitePlansSnippet
                    plan={getSitePlan()}
                />
            </AppProvider>,
            { viewport: { width: 360, height: 240 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует сниппет со схлопнутыми этажами', async() => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <SitePlansSnippet
                    plan={getSitePlan({
                        floors: [ 1, 2, 3, 5, 8, 9, 10, 11 ]
                    })}
                />
            </AppProvider>,
            { viewport: { width: 360, height: 240 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует "сданный" сниппет ', async() => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <SitePlansSnippet
                    plan={getSitePlan({
                        commissioningDate: [
                            {
                                year: 2019,
                                quarter: 3,
                                constructionState: 'CONSTRUCTION_STATE_FINISHED'
                            },
                            {
                                year: 2019,
                                quarter: 4,
                                constructionState: 'CONSTRUCTION_STATE_FINISHED'
                            }
                        ]
                    })}
                />
            </AppProvider>,
            { viewport: { width: 360, height: 240 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует сниппет со сданными корпусами (один)', async() => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <SitePlansSnippet
                    plan={getSitePlan({
                        commissioningDate: [
                            {
                                year: 2019,
                                quarter: 4,
                                constructionState: 'CONSTRUCTION_STATE_FINISHED'
                            },
                            {
                                year: 2020,
                                quarter: 4,
                                constructionState: 'CONSTRUCTION_STATE_UNKNOWN'
                            }
                        ]
                    })}
                />
            </AppProvider>,
            { viewport: { width: 360, height: 240 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует сниппет со сданными корпусами (диапазон)', async() => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <SitePlansSnippet
                    plan={getSitePlan({
                        commissioningDate: [
                            {
                                year: 2019,
                                quarter: 4,
                                constructionState: 'CONSTRUCTION_STATE_FINISHED'
                            },
                            {
                                year: 2020,
                                quarter: 3,
                                constructionState: 'CONSTRUCTION_STATE_UNKNOWN'
                            },
                            {
                                year: 2020,
                                quarter: 4,
                                constructionState: 'CONSTRUCTION_STATE_UNKNOWN'
                            }
                        ]
                    })}
                />
            </AppProvider>,
            { viewport: { width: 360, height: 240 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
