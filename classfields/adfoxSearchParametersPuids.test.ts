import breadcrumbsMock from 'auto-core/react/dataDomain/breadcrumbsPublicApi/mocks/breadcrumbsPublicApi.mock';

import type { TSearchParameters } from 'auto-core/types/TSearchParameters';
import type { TBreadcrumbsMarkLevel } from 'auto-core/types/TBreadcrumbs';

import adfoxSearchParametersPuids from './adfoxSearchParametersPuids';

const { entities: breadcrumbsMarks } = breadcrumbsMock.data[3] as TBreadcrumbsMarkLevel;

it('должен вернуть puid1 и puid10, если не выбрал МММ', () => {
    const searchParameters: TSearchParameters = {
        category: 'moto',
        section: 'all',
    };
    expect(adfoxSearchParametersPuids(searchParameters, breadcrumbsMarks)).toEqual({
        puid1: 'all',
        puid2: '',
        puid3: '',
        puid10: '3',
    });
});

it('должен вернуть puid1, puid2 и puid10, если выбрана марка', () => {
    const searchParameters: TSearchParameters = {
        catalog_filter: [ { mark: 'AUDI' } ],
        category: 'trucks',
        section: 'all',
    };
    expect(adfoxSearchParametersPuids(searchParameters, breadcrumbsMarks)).toEqual({
        puid1: 'all',
        puid2: '3139',
        puid3: '',
        puid10: '2',
    });
});

it('должен вернуть puid1, puid2, puid3 и puid10, если выбрана марка и модель', () => {
    const searchParameters: TSearchParameters = {
        catalog_filter: [ { mark: 'AUDI', model: 'A4' } ],
        category: 'cars',
        section: 'all',
    };
    expect(adfoxSearchParametersPuids(searchParameters, breadcrumbsMarks)).toEqual({
        puid1: 'all',
        puid2: '3139',
        puid3: 'A4',
        puid10: '1',
    });
});

it('должен вернуть puid1, puid2, puid3 и puid10, если выбраны несколько марок и моделей, и удалить дубликаты', () => {
    const searchParameters: TSearchParameters = {
        catalog_filter: [
            { mark: 'AUDI', model: 'A1' },
            { mark: 'AUDI', model: 'A4' },
            { mark: 'BMW' },
        ],
        category: 'trucks',
        section: 'all',
    };
    expect(adfoxSearchParametersPuids(searchParameters, breadcrumbsMarks)).toEqual({
        puid1: 'all',
        puid2: '3139:3141',
        puid3: 'A1:A4',
        puid10: '2',
    });
});
