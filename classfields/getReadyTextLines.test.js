const getReadyTextLines = require('./getReadyTextLines');

const QUESTIONS = [
    {
        answers: [
            {
                answer: 'От',
                search_param: 'year_from',
                ready_prefix: 'Вы выбрали автомобили годов выпуска',
                ready_text: 'от { year_from }',
            },
            {
                answer: 'До',
                search_param: 'year_to',
                ready_prefix: 'Вы выбрали автомобили годов выпуска',
                ready_text: 'до { year_to }',
            },
        ],
    },
    {
        answers: [
            {
                answer: 'От',
                search_param: 'price_from',
                ready_prefix: 'В бюджете',
                ready_text: 'от { price_from|price }',
            },
            {
                answer: 'До',
                search_param: 'price_to',
                ready_prefix: 'В бюджете',
                ready_text: 'до { price_to|price }',
            },
        ],
    },
    {
        answers: [
            {
                answer: 'Хороший',
                catalog_filter: 'AUDI',
                ready_prefix: 'Вы выбрали',
                ready_text: 'хороший автомобиль',
            },
            {
                answer: 'Обычный',
                catalog_filter: 'VAZ',
                ready_prefix: 'вы выбрали',
                ready_text: 'просто автомобиль',
            },
        ],
    },
];

const ANSWER_VALUES = [
    [
        'От=2005',
        'До=2020',
    ],
    [
        'От=10000',
        'До=300000000',
    ],
    [ 'Хороший' ],
];

it('должен вернуть тексты для блока "Готово"', () => {
    expect(getReadyTextLines(QUESTIONS, ANSWER_VALUES)).toMatchSnapshot();
});

it('не должен вернуть текст, если нет значений ответов', () => {
    const EMPTY_ANSWER_VALUES = [
        [ '', '' ],
    ];
    expect(getReadyTextLines(QUESTIONS, EMPTY_ANSWER_VALUES)).toMatchSnapshot();
});
