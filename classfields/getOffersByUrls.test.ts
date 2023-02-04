import de from 'descript';
import _ from 'lodash';

import createHttpReq from 'autoru-frontend/mocks/createHttpReq';
import createHttpRes from 'autoru-frontend/mocks/createHttpRes';

import publicApi from 'auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures';
import type { TDescriptContext } from 'auto-core/server/descript/createContext';

import * as paramsMock from './mocks/params.mocks';
import mockPublicApi from './mocks/publicApi.mocks';
import getOffersByUrls from './getOffersByUrls';

const contextMock = { req: createHttpReq(), res: createHttpRes() } as unknown as TDescriptContext;

it('правильно строит фильтры и накапливает офферы', async() => {
    mockPublicApi(paramsMock.breadcrumbs.mark.mmm, 3);
    mockPublicApi(paramsMock.breadcrumbs.model.mmm, 2);
    mockPublicApi(paramsMock.breadcrumbs.generation.mmm, 1);

    const block = await de.run(getOffersByUrls, { context: contextMock, params: [
        paramsMock.urls.generation,
        paramsMock.urls.model,
        paramsMock.urls.mark,
    ] });

    expect(block.offers).toHaveLength(6);
    expect(block.listingSearchParameters).toMatchSnapshot();
});

it('возвращает правильные фильтры, сразу после первой ссылки', async() => {
    publicApi
        .get('/1.0/search/cars')
        .query(query => {
            return _.isEqual(query, paramsMock.urls.allFilters.filters);
        })
        .reply(200, {
            offers: Array(5)
                .fill(null)
                .map((_, index) => ({ additional_info: {}, id: `${ index }inRegion-rid213` })),
        });

    const block = await de.run(getOffersByUrls, { context: contextMock, params: [
        paramsMock.urls.allFilters.url,
        paramsMock.urls.model,
        paramsMock.urls.mark,
    ] });

    expect(block.listingSearchParameters).toMatchSnapshot();
});

it('возвращает ошибку, если не переданы ссылки', async() => {
    return await de.run(getOffersByUrls, { context: contextMock, params: [] }).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (result) => {
            expect(result.error.id).toBe('ALL_BLOCKS_FAILED');
        });
});
