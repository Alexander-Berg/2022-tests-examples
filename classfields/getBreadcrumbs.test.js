const getBreadcrumbs = require('./getBreadcrumbs');

const QUESTIONS = [
    {
        breadcrumb: 'Вопрос 1',
    },
    {
        breadcrumb: 'Вопрос 2',
    },
    {
        breadcrumb: 'Вопрос 3',
    },
    {
        breadcrumb: 'Вопрос 4',
    },
    {
        breadcrumb: 'Вопрос 5',
        answers_type: 'checkbox',
    },
    {
        breadcrumb: 'Вопрос 6',
    },
];

const ANSWER_VALUES = [
    [ 'Ответ 1' ],
    [ 'Ответ 2' ],
    [ '_' ],
    [ '_' ],
    [ '_' ],
    [ 'Ответ 6' ],
];

it('должен вернуть список крошек с пропуском пропущенных вопросов', () => {
    const questionIndex = 4;
    expect(getBreadcrumbs(QUESTIONS, ANSWER_VALUES, questionIndex)).toStrictEqual([
        {
            title: 'Вопрос 1',
            questionIndex: 0,
        },
        {
            title: 'Вопрос 2',
            questionIndex: 1,
        },
    ]);
});
