import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'view/react/libs/test-helpers';

import { CardPlansOffers } from '../index';

import { offers, turnoverOccurrence, pager, initialState } from './mocks';

const noop = () => {};

describe('CardPlansOffers', () => {
    it('рисует таблицу квартир с фильтром, сортировкой и офферами', async() => {
        await render(
            <AppProvider initialState={initialState}>
                <CardPlansOffers
                    siteId={123}
                    siteFlatPlanId='1234'
                    price={4767090}
                    offers={offers}
                    loadOffers={noop}
                    pager={pager}
                    page={0}
                    isLoading={false}
                    sort='PRICE'
                    direction='ASC'
                    apartmentsType='FLATS'
                    selectedHouses={[]}
                    setNextPage={noop}
                    setSort={noop}
                    setHouse={noop}
                    reset={noop}
                    turnoverOccurrence={turnoverOccurrence}
                />
            </AppProvider>
            ,
            { viewport: { width: 800, height: 600 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует таблицу квартир с фильтром, сортировкой и офферами, вторая страница пагинации', async() => {
        await render(
            <AppProvider initialState={initialState}>
                <CardPlansOffers
                    siteId={123}
                    siteFlatPlanId='1234'
                    price={4767090}
                    offers={offers.slice(7)}
                    loadOffers={noop}
                    pager={{
                        page: 1,
                        pageSize: 10,
                        sitesPageSize: 10,
                        totalItems: 13,
                        totalPages: 2
                    }}
                    page={1}
                    isLoading={false}
                    sort='PRICE'
                    direction='ASC'
                    apartmentsType='FLATS'
                    selectedHouses={[]}
                    setNextPage={noop}
                    setSort={noop}
                    setHouse={noop}
                    reset={noop}
                    turnoverOccurrence={turnoverOccurrence}
                />
            </AppProvider>
            ,
            { viewport: { width: 800, height: 600 } }
        );

        await page.click('.CardPlansOffers__showMoreBtn');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует таблицу квартир с фильтром, сортировкой и офферами, показать еще 1 квартиру', async() => {
        const totalItems = 5;

        await render(
            <AppProvider initialState={initialState}>
                <CardPlansOffers
                    siteId={123}
                    siteFlatPlanId='1234'
                    price={4767090}
                    offers={offers.slice(0, totalItems)}
                    loadOffers={noop}
                    pager={{
                        page: 0,
                        pageSize: 10,
                        sitesPageSize: 10,
                        totalItems,
                        totalPages: 1
                    }}
                    page={0}
                    isLoading={false}
                    sort='PRICE'
                    direction='ASC'
                    apartmentsType='FLATS'
                    selectedHouses={[]}
                    setNextPage={noop}
                    setSort={noop}
                    setHouse={noop}
                    reset={noop}
                    turnoverOccurrence={turnoverOccurrence}
                />
            </AppProvider>
            ,
            { viewport: { width: 800, height: 600 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует таблицу квартир с фильтром, сортировкой и офферами, показать еще 2 квартиры', async() => {
        const totalItems = 6;

        await render(
            <AppProvider initialState={initialState}>
                <CardPlansOffers
                    siteId={123}
                    siteFlatPlanId='1234'
                    price={4767090}
                    offers={offers.slice(0, totalItems)}
                    loadOffers={noop}
                    pager={{
                        page: 0,
                        pageSize: 10,
                        sitesPageSize: 10,
                        totalItems,
                        totalPages: 1
                    }}
                    page={0}
                    isLoading={false}
                    sort='PRICE'
                    direction='ASC'
                    apartmentsType='FLATS'
                    selectedHouses={[]}
                    setNextPage={noop}
                    setSort={noop}
                    setHouse={noop}
                    reset={noop}
                    turnoverOccurrence={turnoverOccurrence}
                />
            </AppProvider>
            ,
            { viewport: { width: 800, height: 600 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует таблицу апартаментов с фильтром, сортировкой и офферами', async() => {
        await render(
            <AppProvider initialState={initialState}>
                <CardPlansOffers
                    siteId={123}
                    siteFlatPlanId='1234'
                    price={4767090}
                    offers={offers}
                    loadOffers={noop}
                    pager={pager}
                    page={0}
                    isLoading={false}
                    sort='PRICE'
                    direction='ASC'
                    apartmentsType='APARTMENTS'
                    selectedHouses={[]}
                    setNextPage={noop}
                    setSort={noop}
                    setHouse={noop}
                    reset={noop}
                    turnoverOccurrence={turnoverOccurrence}
                />
            </AppProvider>
            ,
            { viewport: { width: 800, height: 600 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует таблицу апартаментов с фильтром, сортировкой и офферами, показать еще 1 апартамент', async() => {
        const totalItems = 5;

        await render(
            <AppProvider initialState={initialState}>
                <CardPlansOffers
                    siteId={123}
                    siteFlatPlanId='1234'
                    price={4767090}
                    offers={offers.slice(0, totalItems)}
                    loadOffers={noop}
                    pager={{
                        page: 0,
                        pageSize: 10,
                        sitesPageSize: 10,
                        totalItems,
                        totalPages: 1
                    }}
                    page={0}
                    isLoading={false}
                    sort='PRICE'
                    direction='ASC'
                    apartmentsType='APARTMENTS'
                    selectedHouses={[]}
                    setNextPage={noop}
                    setSort={noop}
                    setHouse={noop}
                    reset={noop}
                    turnoverOccurrence={turnoverOccurrence}
                />
            </AppProvider>
            ,
            { viewport: { width: 800, height: 600 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует таблицу апартаментов с фильтром, сортировкой и офферами, показать еще 2 апартамента', async() => {
        const totalItems = 6;

        await render(
            <AppProvider initialState={initialState}>
                <CardPlansOffers
                    siteId={123}
                    siteFlatPlanId='1234'
                    price={4767090}
                    offers={offers.slice(0, totalItems)}
                    loadOffers={noop}
                    pager={{
                        page: 0,
                        pageSize: 10,
                        sitesPageSize: 10,
                        totalItems,
                        totalPages: 1
                    }}
                    page={0}
                    isLoading={false}
                    sort='PRICE'
                    direction='ASC'
                    apartmentsType='APARTMENTS'
                    selectedHouses={[]}
                    setNextPage={noop}
                    setSort={noop}
                    setHouse={noop}
                    reset={noop}
                    turnoverOccurrence={turnoverOccurrence}
                />
            </AppProvider>
            ,
            { viewport: { width: 800, height: 600 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует таблицу смешанной выдачи с фильтром, сортировкой и офферами', async() => {
        await render(
            <AppProvider initialState={initialState}>
                <CardPlansOffers
                    siteId={123}
                    siteFlatPlanId='1234'
                    price={4767090}
                    offers={offers}
                    loadOffers={noop}
                    pager={pager}
                    page={0}
                    isLoading={false}
                    sort='PRICE'
                    direction='ASC'
                    apartmentsType='APARTMENTS_AND_FLATS'
                    selectedHouses={[]}
                    setNextPage={noop}
                    setSort={noop}
                    setHouse={noop}
                    reset={noop}
                    turnoverOccurrence={turnoverOccurrence}
                />
            </AppProvider>
            ,
            { viewport: { width: 800, height: 600 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует таблицу смешанной выдачи с фильтром, сортировкой и офферами, показать еще 1 предложение', async() => {
        const totalItems = 5;

        await render(
            <AppProvider initialState={initialState}>
                <CardPlansOffers
                    siteId={123}
                    siteFlatPlanId='1234'
                    price={4767090}
                    offers={offers.slice(0, totalItems)}
                    loadOffers={noop}
                    pager={{
                        page: 0,
                        pageSize: 10,
                        sitesPageSize: 10,
                        totalItems,
                        totalPages: 1
                    }}
                    page={0}
                    isLoading={false}
                    sort='PRICE'
                    direction='ASC'
                    apartmentsType='APARTMENTS_AND_FLATS'
                    selectedHouses={[]}
                    setNextPage={noop}
                    setSort={noop}
                    setHouse={noop}
                    reset={noop}
                    turnoverOccurrence={turnoverOccurrence}
                />
            </AppProvider>
            ,
            { viewport: { width: 800, height: 600 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует таблицу смешанной выдачи с фильтром, сортировкой и офферами, показать еще 2 предложения', async() => {
        const totalItems = 6;

        await render(
            <AppProvider initialState={initialState}>
                <CardPlansOffers
                    siteId={123}
                    siteFlatPlanId='1234'
                    price={4767090}
                    offers={offers.slice(0, totalItems)}
                    loadOffers={noop}
                    pager={{
                        page: 0,
                        pageSize: 10,
                        sitesPageSize: 10,
                        totalItems,
                        totalPages: 1
                    }}
                    page={0}
                    isLoading={false}
                    sort='PRICE'
                    direction='ASC'
                    apartmentsType='APARTMENTS_AND_FLATS'
                    selectedHouses={[]}
                    setNextPage={noop}
                    setSort={noop}
                    setHouse={noop}
                    reset={noop}
                    turnoverOccurrence={turnoverOccurrence}
                />
            </AppProvider>
            ,
            { viewport: { width: 800, height: 600 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует раскрытую таблицу квартир с фильтром и сортировкой', async() => {
        await render(
            <AppProvider initialState={initialState}>
                <CardPlansOffers
                    siteId={123}
                    siteFlatPlanId='1234'
                    price={4767090}
                    offers={offers}
                    loadOffers={noop}
                    pager={pager}
                    page={0}
                    isLoading={false}
                    sort='PRICE'
                    direction='ASC'
                    apartmentsType='FLATS'
                    selectedHouses={[]}
                    setNextPage={noop}
                    setSort={noop}
                    setHouse={noop}
                    reset={noop}
                    turnoverOccurrence={turnoverOccurrence}
                />
            </AppProvider>
            ,
            { viewport: { width: 800, height: 1200 } }
        );

        await page.click('.CardPlansOffers__showMoreBtn');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует таблицу с 4 офферами, без сортировки', async() => {
        await render(
            <AppProvider initialState={initialState}>
                <CardPlansOffers
                    siteId={123}
                    siteFlatPlanId='1234'
                    price={4767090}
                    offers={offers.slice(0, 4)}
                    loadOffers={noop}
                    pager={{
                        page: 0,
                        pageSize: 4,
                        sitesPageSize: 4,
                        totalItems: 4,
                        totalPages: 1
                    }}
                    page={0}
                    isLoading={false}
                    sort='PRICE'
                    direction='ASC'
                    apartmentsType='FLATS'
                    selectedHouses={[]}
                    setNextPage={noop}
                    setSort={noop}
                    setHouse={noop}
                    reset={noop}
                    turnoverOccurrence={turnoverOccurrence}
                />
            </AppProvider>
            ,
            { viewport: { width: 800, height: 600 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
