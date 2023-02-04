import breadcrumbsPublicApiMock from 'auto-core/react/dataDomain/breadcrumbsPublicApi/mocks/breadcrumbsPublicApi.mock';

import reducer from './reducer';
import { BREADCRUMBS_PUBLICAPI_MERGE_RESOLVED } from './actionTypes';
import type { StateBreadcrumbsPublicApi, BreadcrumbsMergeResolvedAction } from './types';

it('должен обновить крошки на экшен BREADCRUMBS_PUBLICAPI_MERGE_RESOLVED', () => {
    const breadcrumbsToMerge = [
        {
            entities: [
                {
                    id: 'AEROSTAR',
                },
            ],
            level: 'MODEL_LEVEL',
            levelFilterParams: { mark: 'FORD1' },
            mark: { id: 'FORD1', name: 'Ford1' },
            meta_level: 'MODEL_LEVEL',
        },
    ] as StateBreadcrumbsPublicApi['data'];

    const state = breadcrumbsPublicApiMock;

    const action: BreadcrumbsMergeResolvedAction = {
        type: BREADCRUMBS_PUBLICAPI_MERGE_RESOLVED,
        payload: breadcrumbsToMerge,
    };

    expect(reducer(state, action)).toEqual({
        data: [
            ...breadcrumbsPublicApiMock.data,
            ...breadcrumbsToMerge,
        ],
        status: 'SUCCESS',
    });
});

it('не должен обновить крошки на экшен BREADCRUMBS_PUBLICAPI_MERGE_RESOLVED, если такие уже есть', () => {
    const breadcrumbsToMerge = [
        {
            entities: [
                {
                    id: 'AEROSTAR',
                },
            ],
            level: 'MODEL_LEVEL',
            levelFilterParams: { mark: 'FORD' },
            mark: { id: 'FORD', name: 'Ford' },
            meta_level: 'MODEL_LEVEL',
        },
    ] as StateBreadcrumbsPublicApi['data'];

    const state = breadcrumbsPublicApiMock;

    const action: BreadcrumbsMergeResolvedAction = {
        type: BREADCRUMBS_PUBLICAPI_MERGE_RESOLVED,
        payload: breadcrumbsToMerge,
    };

    expect(reducer(state, action)).toEqual({
        data: [
            ...breadcrumbsPublicApiMock.data,
        ],
        status: 'SUCCESS',
    });
});
