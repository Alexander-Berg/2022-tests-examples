jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(),
    };
});

const prepareRadioAnswers = require('./prepareRadioAnswers');

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
                answer: 'Большой багажник',
                search_tag: 'big_trunk',
            },
            {
                answer: 'Маленький багажник',
                search_tag: 'small_trunk',
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
        answer: 'Большой багажник',
        search_tag: 'big_trunk',
    },
    {
        answer: 'Маленький багажник',
        search_tag: 'small_trunk',
    },
];

const gateApiMock = (resource, params) => {
    if (params.search_tag.includes('big_trunk')) {
        return Promise.resolve({
            grouping: {
                groups_count: 6,
            },
        });
    }
    return Promise.resolve({
        grouping: {
            groups_count: 5,
        },
    });
};
getResource.mockImplementation(gateApiMock);

it('должен вернуть список ответов, у которых по результатам запроса в search есть больше 5 групп', () => {
    return prepareRadioAnswers(QUESTIONS, ANSWER_VALUES, ANSWERS).then(
        result => {
            expect(result).toStrictEqual([
                {
                    answer: 'Большой багажник',
                    search_tag: 'big_trunk',
                },
            ]);
        },
    );
});
