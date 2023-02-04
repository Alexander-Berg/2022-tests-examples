jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(),
    };
});

import gateApi from 'auto-core/react/lib/gateApi';

import type { TAutoguruQuestion } from '../TAutoguruQuestion';

import getAdditionalModels from './getAdditionalModels';

const QUESTIONS = [
    {
        answers: [
            {
                answer: 'От',
                search_param: 'price_from',
            },
            {
                answer: 'До',
                search_param: 'price_to',
            },
        ],
    } as Partial<TAutoguruQuestion> as TAutoguruQuestion,
    {
        answers: [
            {
                answer: 'Большой багажник',
                search_tag: 'big_trunk',
            },
            {
                answer: 'Маленький багажник',
                search_tag: 'small_trunk',
            },
        ],
    } as Partial<TAutoguruQuestion> as TAutoguruQuestion,
    {
        answers: [
            {
                answer: 'Ликвидный',
                search_tag: 'liquid',
            },
        ],
    } as Partial<TAutoguruQuestion> as TAutoguruQuestion,
];

it('должен вызвать поиск без параметров последнего вопроса', () => {
    const answerValues = [
        [ 'От=1000000', 'До=2000000' ],
        [ 'Большой багажник' ],
        [ 'Ликвидный' ],
    ];

    (gateApi.getResource as jest.Mock).mockImplementation((name) => {
        if (name === 'searchCount') {
            return Promise.resolve({
                grouping: {
                    groups_count: 10,
                },
            });
        }
        return Promise.resolve({ offers: [] });
    });

    return getAdditionalModels(QUESTIONS, answerValues, { exclude_catalog_filter: [ { mark: 'AUDI' } ] }).then(
        () => {
            expect(gateApi.getResource).toHaveBeenNthCalledWith(2, 'search', {
                category: 'cars',
                exclude_catalog_filter: [ { mark: 'AUDI' } ],
                group_by: [ 'CONFIGURATION' ],
                page_size: 10,
                price_from: 1000000,
                price_to: 2000000,
                search_tag: [ 'big_trunk' ],
                section: 'all',
            });
        },
    );
});

it('должен вызвать поиск без параметров двух последних вопросов, если для параметров последнего вопроса вернулось менее 10 групп', () => {
    const answerValues = [
        [ 'От=1000000', 'До=2000000' ],
        [ 'Большой багажник' ],
        [ 'Ликвидный' ],
    ];

    (gateApi.getResource as jest.Mock).mockImplementation((name, params) => {
        if (name === 'searchCount') {
            if (params.search_tag && params.search_tag.includes('big_trunk')) {
                return Promise.resolve({
                    grouping: {
                        groups_count: 9,
                    },
                });
            }
            return Promise.resolve({
                grouping: {
                    groups_count: 10,
                },
            });
        }
        return Promise.resolve({ offers: [] });
    });

    return getAdditionalModels(QUESTIONS, answerValues, { exclude_catalog_filter: [ { mark: 'AUDI' } ] }).then(
        () => {
            expect(gateApi.getResource).toHaveBeenNthCalledWith(3, 'search', {
                category: 'cars',
                exclude_catalog_filter: [ { mark: 'AUDI' } ],
                group_by: [ 'CONFIGURATION' ],
                page_size: 10,
                price_from: 1000000,
                price_to: 2000000,
                section: 'all',
            });
        },
    );
});

it('должен вызвать поиск без параметров двух последних вопросов, если последний ответ пропущен', () => {
    const answerValues = [
        [ 'От=1000000', 'До=2000000' ],
        [ 'Большой багажник' ],
        [ '_' ],
    ];

    (gateApi.getResource as jest.Mock).mockImplementation((name) => {
        if (name === 'searchCount') {
            return Promise.resolve({
                grouping: {
                    groups_count: 10,
                },
            });
        }
        return Promise.resolve({ offers: [] });
    });

    return getAdditionalModels(QUESTIONS, answerValues, { exclude_catalog_filter: [ { mark: 'AUDI' } ] }).then(
        () => {
            expect(gateApi.getResource).toHaveBeenNthCalledWith(2, 'search', {
                category: 'cars',
                exclude_catalog_filter: [ { mark: 'AUDI' } ],
                group_by: [ 'CONFIGURATION' ],
                page_size: 10,
                price_from: 1000000,
                price_to: 2000000,
                section: 'all',
            });
        },
    );
});
