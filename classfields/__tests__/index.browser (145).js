import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'view/react/libs/test-helpers';

import SitePlansSnippet from '..';

import { getSitePlan } from './mocks';

describe('SitePlansSnippet', () => {
    it('рисует сниппет в состоянии загрузки', async() => {
        await render(
            <AppProvider>
                <SitePlansSnippet
                    plan={getSitePlan()}
                    isLoading
                    apartmentsType="FLATS"
                />
            </AppProvider>,
            { viewport: { width: 840, height: 360 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует сниппет без изображения', async() => {
        await render(
            <AppProvider>
                <SitePlansSnippet
                    apartmentsType="FLATS"
                    plan={getSitePlan({ withImages: false })}
                />
            </AppProvider>,
            { viewport: { width: 840, height: 360 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует сниппет студии', async() => {
        await render(
            <AppProvider>
                <SitePlansSnippet
                    apartmentsType="FLATS"
                    plan={getSitePlan({ isStudio: true })}
                />
            </AppProvider>,
            { viewport: { width: 840, height: 360 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует сниппет без площади кухни', async() => {
        await render(
            <AppProvider>
                <SitePlansSnippet
                    apartmentsType="FLATS"
                    plan={getSitePlan({ withKitchenArea: false })}
                />
            </AppProvider>,
            { viewport: { width: 840, height: 360 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует сниппет с 1 оффером', async() => {
        await render(
            <AppProvider>
                <SitePlansSnippet
                    apartmentsType="FLATS"
                    plan={getSitePlan({
                        offersCount: 1
                    })}
                />
            </AppProvider>,
            { viewport: { width: 840, height: 360 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует сниппет с 1 оффером (апартамент)', async() => {
        await render(
            <AppProvider>
                <SitePlansSnippet
                    apartmentsType="APARTMENTS"
                    plan={getSitePlan({
                        offersCount: 1
                    })}
                />
            </AppProvider>,
            { viewport: { width: 840, height: 360 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует сниппет с 1 оффером (смешанная выдача)', async() => {
        await render(
            <AppProvider>
                <SitePlansSnippet
                    apartmentsType="APARTMENTS_AND_FLATS"
                    plan={getSitePlan({
                        offersCount: 1
                    })}
                />
            </AppProvider>,
            { viewport: { width: 840, height: 360 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует сниппет с несколькими офферами', async() => {
        await render(
            <AppProvider>
                <SitePlansSnippet
                    apartmentsType="FLATS"
                    plan={getSitePlan()}
                />
            </AppProvider>,
            { viewport: { width: 840, height: 360 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует сниппет с несколькими офферами (апартаменты)', async() => {
        await render(
            <AppProvider>
                <SitePlansSnippet
                    apartmentsType="APARTMENTS"
                    plan={getSitePlan()}
                />
            </AppProvider>,
            { viewport: { width: 840, height: 360 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует сниппет с несколькими офферами (смешанная выдача)', async() => {
        await render(
            <AppProvider>
                <SitePlansSnippet
                    apartmentsType="APARTMENTS_AND_FLATS"
                    plan={getSitePlan()}
                />
            </AppProvider>,
            { viewport: { width: 840, height: 360 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует сниппет со схлопнутыми этажами', async() => {
        await render(
            <AppProvider>
                <SitePlansSnippet
                    apartmentsType="FLATS"
                    plan={getSitePlan({
                        floors: [ 1, 2, 3, 5, 8, 9, 10, 11 ]
                    })}
                />
            </AppProvider>,
            { viewport: { width: 840, height: 360 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует "сданный" сниппет ', async() => {
        await render(
            <AppProvider>
                <SitePlansSnippet
                    apartmentsType="FLATS"
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
            { viewport: { width: 840, height: 360 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует сниппет со сданными корпусами (один)', async() => {
        await render(
            <AppProvider>
                <SitePlansSnippet
                    apartmentsType="FLATS"
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
            { viewport: { width: 840, height: 360 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует сниппет со сданными корпусами (диапазон)', async() => {
        await render(
            <AppProvider>
                <SitePlansSnippet
                    apartmentsType="FLATS"
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
            { viewport: { width: 840, height: 360 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
