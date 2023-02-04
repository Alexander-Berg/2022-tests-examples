const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const AutoGuruQuestionAnswersCheckbox = require('./AutoGuruQuestionAnswersCheckbox');
const onAnswerMock = jest.fn();

const ANSWERS = [
    {
        answer: 'первый вариант',
        search_tag: [
            'wide-back-seats',
        ],
        search_tag_exclude: [],
    },
    {
        answer: 'второй вариант',
        body_type_group: [
            '',
        ],
        search_tag: [
            'big-trunk',
        ],
    },
];

let tree;
beforeEach(() => {
    tree = shallow(
        <AutoGuruQuestionAnswersCheckbox
            answers={ ANSWERS }
            onAnswer={ onAnswerMock }
        />,
    );
});

it('должен отрендерить варианты ответа с чекбоксами и кнопкой "Далее"', () => {
    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('должен передать список выбранных ответов при клике по кнопке "Далее"', () => {
    const answerIndex = 1;
    tree.find('CheckboxGroup').simulate('change', [ answerIndex ]);
    tree.find('Button').simulate('click');
    expect(onAnswerMock).toHaveBeenCalledWith([ ANSWERS[answerIndex] ]);
});

it('должен передать пустой список при клике по кнопке "Далее", если ни один ответ не выбран', () => {
    tree.find('Button').simulate('click');
    expect(onAnswerMock).toHaveBeenCalledWith([]);
});
