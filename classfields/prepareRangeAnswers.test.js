jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(),
    };
});

const prepareRangeAnswers = require('./prepareRangeAnswers');

const getResource = require('auto-core/react/lib/gateApi').getResource;

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
    },
    {
        answers: [
            {
                answer: 'От',
                search_param: 'year_from',
            },
            {
                answer: 'До',
                search_param: 'year_to',
            },
        ],
    },
    {
        answers: [
            {
                answer: 'Не важно',
                search_tag: 'xxx',
            },
        ],
    },
];

const ANSWER_VALUES = [
    [ 'От=1000000', 'До=2000000' ],
];

const ANSWERS = [
    {
        answer: 'От',
        search_param: 'year_from',
        default_value: 2005,
    },
    {
        answer: 'До',
        search_param: 'year_to',
        default_value: 2010,
    },
];

const gateApiMock = (resource, params) => {
    if (params.sort.endsWith('-ASC')) {
        return Promise.resolve({
            offers: [
                {
                    documents: {
                        year: 2000,
                    },
                },
            ],
        });
    }
    return Promise.resolve({
        offers: [
            {
                documents: {
                    year: 2016,
                },
            },
        ],
    });

};
getResource.mockImplementation(gateApiMock);

it('должен вернуть список ответов с полем default_value, измененным в соответствии с результатами поиска', () => {
    return prepareRangeAnswers(QUESTIONS, ANSWER_VALUES, ANSWERS).then(
        result => {
            expect(result).toStrictEqual([
                {
                    answer: 'От',
                    search_param: 'year_from',
                    default_value: 2005,
                },
                {
                    answer: 'До',
                    search_param: 'year_to',
                    default_value: 2016,
                },
            ]);
        },
    );
});
