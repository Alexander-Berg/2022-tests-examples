import de from 'descript';

import createHttpReq from 'autoru-frontend/mocks/createHttpReq';
import createHttpRes from 'autoru-frontend/mocks/createHttpRes';

import publicApi from 'auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures';
import type { TDescriptContext } from 'auto-core/server/descript/createContext';

import * as paramsMock from './mocks/params.mocks';
import mockPublicApi from './mocks/publicApi.mocks';
import getOffersAutoru from './getOffersAutoru';

const contextMock = {
    req: {
        ...createHttpReq(),
        geoIds: [ 213 ],
        geoParents: [],
        geoIdsInfo: [],
    },
    res: createHttpRes(),
} as unknown as TDescriptContext;

it('отфильтрует дублированные офферы', async() => {
    publicApi
        .get('/1.0/search/cars')
        .query(query => {
            return query.catalog_filter === paramsMock.breadcrumbs.generation.mmm;
        })
        .reply(200, {
            offers: Array(20)
                .fill(null)
                .map((_, index) => ({ additional_info: {}, id: `${ index < 5 ? 0 : index }inRegion-rid213` })),
            pagination: {
                page: 1,
                page_size: 20,
                total_offers_count: 20,
            },
        });

    const block = await de.run(
        getOffersAutoru,
        { context: contextMock, params: { urls: [ paramsMock.urls.generation ] } },
    );

    expect(block.offers[0].id).toBe('0inRegion');
    expect(block.offers[1].id).toBe('5inRegion');
});

describe('не вернёт офферы, если', () => {
    it('не переданы параметры', async() => {
        const block = await de.run(getOffersAutoru, {});

        expect(block.offers).toHaveLength(0);
    });

    it('не переданы ссылки для поиска', async() => {
        const block = await de.run(getOffersAutoru, { params: { urls: [] } });

        expect(block.offers).toHaveLength(0);
    });

    it('количество найденных офферов по крошкам меньше 4', async() => {
        mockPublicApi(paramsMock.breadcrumbs.generation.mmm);

        const block = await de.run(
            getOffersAutoru,
            { context: contextMock, params: { breadcrumbs: paramsMock.breadcrumbs.generation.filters } },
        );

        expect(block.offers).toHaveLength(0);
    });

    it('количество найденных офферов по урлам меньше 4', async() => {
        mockPublicApi(paramsMock.breadcrumbs.generation.mmm);

        const block = await de.run(
            getOffersAutoru,
            { context: contextMock, params: { urls: [ paramsMock.urls.generation ] } },
        );

        expect(block.offers).toHaveLength(0);
    });
});
