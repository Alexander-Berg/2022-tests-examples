import { getBunkerMock } from 'autoru-frontend/mockData/state/bunker.mock';

import type { StateBreadcrumbsPublicApi } from 'auto-core/react/dataDomain/breadcrumbsPublicApi/types';
import type { StateBunker } from 'auto-core/react/dataDomain/bunker/StateBunker';
import breadcrumbsPublicApiMock from 'auto-core/react/dataDomain/breadcrumbsPublicApi/mocks/breadcrumbsPublicApi.mock';

import getMarkModelsWithCount from './getMarkModelsWithCount';

interface TAppState {
    breadcrumbsPublicApi: StateBreadcrumbsPublicApi;
    bunker: StateBunker;
}

const node = 'common/new4new';

it('должен вернуть список марок с моделями', () => {
    const state: TAppState = {
        bunker: {
            [node]: getBunkerMock([ node ])[ node ].slice(0, 3),
        },
        breadcrumbsPublicApi: breadcrumbsPublicApiMock as StateBreadcrumbsPublicApi,
    };

    expect(getMarkModelsWithCount(state)).toEqual([
        { catalog_filter: [ { mark: 'FORD', model: 'S_MAX' } ], count: 9, id: 'FORD-S_MAX', name: 'Ford S-MAX' },
        { catalog_filter: [ { mark: 'FORD', model: 'SCORPIO' } ], count: 1, id: 'FORD-SCORPIO', name: 'Ford Scorpio' },
        { catalog_filter: [ { mark: 'FORD', model: 'SIERRA' } ], count: 1, id: 'FORD-SIERRA', name: 'Ford Sierra' },
    ]);
});
