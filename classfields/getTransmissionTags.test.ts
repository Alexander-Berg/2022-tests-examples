import getTransmissionTags from 'auto-core/react/dataDomain/crossLinks/helpers/getTransmissionTags';

import type { TSearchParameters } from 'auto-core/types/TSearchParameters';
import type { TBreadcrumbsGeneration, TBreadcrumbsMark, TBreadcrumbsModel } from 'auto-core/types/TBreadcrumbs';

type MmmInfo = {
    mark?: TBreadcrumbsMark;
    model?: TBreadcrumbsModel;
    generation?: TBreadcrumbsGeneration;
}

const transmissionTypes =
    [
        {
            key: 'FORD_ROBOT',
            transmission: 'ROBOT',
            transmissionRus: 'роботом',
            value: 'ROBOT',
        },
        {
            key: 'FORD_AUTOMATIC',
            transmission: 'AUTOMATIC',
            transmissionRus: 'АКПП',
            value: 'AUTOMATIC',
        },
        {
            key: 'FORD_MECHANICAL',
            transmission: 'MECHANICAL',
            transmissionRus: 'МКПП',
            value: 'MECHANICAL',
        },
        {
            key: 'FORD_VARIATOR',
            transmission: 'VARIATOR',
            transmissionRus: 'вариатором',
            value: 'VARIATOR',
        },
    ];

it('Должен вернуть пустой массив, если не выбрана марка', () => {
    const searchParameters: TSearchParameters = {
        section: 'all',
        category: 'cars',
    };

    const mmmInfo = {};

    expect(getTransmissionTags(searchParameters, mmmInfo, transmissionTypes)).toEqual([]);
});

it('Должен вернуть трансмиссии из transmissionTypes для марки BMW', () => {
    const searchParameters: TSearchParameters = {
        catalog_filter: [
            {
                mark: 'BMW',
            },
        ],
        section: 'all',
        category: 'cars',
    };

    const mmmInfo: MmmInfo = {
        mark: {
            cyrillic_name: 'BMW',
            'big-logo': '',
            count: 10,
            reviews_count: 12,
            id: '1',
            logo: '',
            name: 'BMW',
            numeric_id: 1,
            popular: true,
            itemFilterParams: { mark: 'BMW' },
        },
    };

    expect(getTransmissionTags(searchParameters, mmmInfo, transmissionTypes)).toMatchSnapshot();
});

it('Должен вернуть трансмиссии из transmissionTypes для марки BMW и модели 3ER', () => {
    const searchParameters: TSearchParameters = {
        catalog_filter: [
            {
                mark: 'BMW',
                model: '3ER',
            },
        ],
        section: 'all',
        category: 'cars',
    };

    const mmmInfo: MmmInfo = {
        mark: {
            cyrillic_name: 'BMW',
            'big-logo': '',
            count: 10,
            reviews_count: 12,
            id: '1',
            logo: '',
            name: 'BMW',
            numeric_id: 1,
            popular: true,
            itemFilterParams: { mark: 'BMW' },
        },
        model: {
            count: 2794,
            cyrillic_name: '3 серии',
            id: '3ER',
            itemFilterParams: { model: '3ER' },
            name: '3 серии',
            nameplates: [
                { id: '9264870', name: '315', semantic_url: '315', no_model: false },
                { id: '9264871', name: '316', semantic_url: '316', no_model: false },
            ],
            popular: false,
            reviews_count: 709,
            year_from: 1975,
            year_to: 2022,
        },
    };

    expect(getTransmissionTags(searchParameters, mmmInfo, transmissionTypes)).toMatchSnapshot();
});
