const getAnswersFromAnswerValues = require('./getAnswersFromAnswerValues');

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
                search_tag: '',
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
    [ 'Большой багажник' ],
];

it('должен вернуть список ответов на основе списка вопросов и списка пользовательских ответов', () => {
    expect(getAnswersFromAnswerValues(QUESTIONS, ANSWER_VALUES)).toStrictEqual([
        [
            {
                answer: 'От',
                search_param: 'price_from',
                price_from: '1000000',
            },
            {
                answer: 'До',
                search_param: 'price_to',
                price_to: '2000000',
            },
        ],
        [
            {
                answer: 'Большой багажник',
                search_tag: 'big_trunk',
            },
        ],
    ]);
});
