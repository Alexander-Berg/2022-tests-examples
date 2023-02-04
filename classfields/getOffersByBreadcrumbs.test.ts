import de from 'descript';

import createHttpReq from 'autoru-frontend/mocks/createHttpReq';
import createHttpRes from 'autoru-frontend/mocks/createHttpRes';

import type { TDescriptContext } from 'auto-core/server/descript/createContext';

import { breadcrumbs as paramsMock } from './mocks/params.mocks';
import mockPublicApi from './mocks/publicApi.mocks';
import getOffersByBreadcrumbs from './getOffersByBreadcrumbs';

const contextMock = { req: createHttpReq(), res: createHttpRes() } as unknown as TDescriptContext;

describe('вернёт офферы и параметры, если', () => {
    it('найдены по фильтру: марка', async() => {
        mockPublicApi(paramsMock.mark.mmm, 15);

        const block = await de.run(
            getOffersByBreadcrumbs,
            { context: contextMock, params: paramsMock.mark.filters },
        );
        expect(block.offers).toHaveLength(12);
        expect(block.listingSearchParameters).toMatchSnapshot();
    });

    it('найдены по фильтру: марка, модель', async() => {
        mockPublicApi(paramsMock.model.mmm, 15);

        const block = await de.run(
            getOffersByBreadcrumbs,
            { context: contextMock, params: paramsMock.model.filters },
        );
        expect(block.offers).toHaveLength(12);
        expect(block.listingSearchParameters).toMatchSnapshot();
    });

    it('найдены по фильтру: марка, модель, поколение', async() => {
        mockPublicApi(paramsMock.generation.mmm, 15);

        const block = await de.run(
            getOffersByBreadcrumbs,
            { context: contextMock, params: paramsMock.generation.filters },
        );
        expect(block.offers).toHaveLength(12);
        expect(block.listingSearchParameters).toMatchSnapshot();
    });
});

describe('правильно строит фильтры накапливает офферы по фильтрам', () => {
    beforeEach(() => {
        mockPublicApi(paramsMock.mark.mmm, 3);
        mockPublicApi(paramsMock.model.mmm, 2);
    });

    it('марка + модель', async() => {
        const block = await de.run(
            getOffersByBreadcrumbs,
            { context: contextMock, params: paramsMock.model.filters },
        );
        expect(block.offers).toHaveLength(5);
        expect(block.listingSearchParameters.catalog_filter?.[0]).toMatchSnapshot();
    });

    it('марка + модель + поколение', async() => {
        mockPublicApi(paramsMock.generation.mmm, 3);

        const block = await de.run(
            getOffersByBreadcrumbs, { context: contextMock, params: paramsMock.generation.filters },
        );
        expect(block.offers).toHaveLength(8);
        expect(block.listingSearchParameters.catalog_filter?.[0]).toMatchSnapshot();
    });
});

it('вернёт 0 офферов и фильтров, если не найдены офферы', async() => {
    const block = await de.run(
        getOffersByBreadcrumbs, { context: contextMock, params: paramsMock.mark.filters },
    );

    expect(block.offers).toHaveLength(0);
});
