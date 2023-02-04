import getSeatsTags from 'auto-core/react/dataDomain/crossLinks/helpers/getSeatsTags';

import type { TSearchParameters } from 'auto-core/types/TSearchParameters';
import type { TBreadcrumbsGeneration, TBreadcrumbsMark, TBreadcrumbsModel } from 'auto-core/types/TBreadcrumbs';

type MmmInfo = {
    mark?: TBreadcrumbsMark;
    model?: TBreadcrumbsModel;
    generation?: TBreadcrumbsGeneration;
}

const seatsTypes = [
    {
        humanSeats: '2-местный',
        key: 'BMW_2',
        value: 'seats-2',
    },
    {
        humanSeats: '5-местный',
        key: 'BMW_5',
        value: 'seats-5',
    },
    {
        humanSeats: '6-местный',
        key: 'BMW_6',
        value: 'seats-6',
    },
    {
        humanSeats: '7-местный',
        key: 'BMW_7',
        value: 'seats-7',
    },
];

it('Должен вернуть пустой массив, если не выбрана марка', () => {
    const searchParameters: TSearchParameters = {
        section: 'all',
        category: 'cars',
    };

    const mmmInfo = {};

    expect(getSeatsTags(searchParameters, mmmInfo, seatsTypes)).toEqual([]);
});

it('Должен вернуть количество сидений из seatsTypes для марки BMW', () => {
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

    expect(getSeatsTags(searchParameters, mmmInfo, seatsTypes)).toMatchSnapshot();
});

it('Должен вернуть количество сидений из seatsTypes для марки BMW и модели 3ER', () => {
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

    expect(getSeatsTags(searchParameters, mmmInfo, seatsTypes)).toMatchSnapshot();
});
