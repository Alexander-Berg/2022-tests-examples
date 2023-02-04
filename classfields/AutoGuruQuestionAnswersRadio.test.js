const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const AutoGuruQuestionAnswersRadio = require('./AutoGuruQuestionAnswersRadio');
const onAnswerMock = jest.fn();

const ANSWERS = [
    {
        answer: 'Да',
        catalog_filter: 'mark=AUDI,model=A4',
    },
    {
        answer: 'Нет',
        exclude_catalog_filter: 'mark=AUDI,model=A4',
    },
];

let tree;
beforeEach(() => {
    tree = shallow(
        <AutoGuruQuestionAnswersRadio
            answers={ ANSWERS }
            onAnswer={ onAnswerMock }
        />,
    );
});

it('должен отрендерить варианты ответа в виде кнопок', () => {
    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('должен передать ответы с установленными пользователеим значениями параметров', () => {
    tree.find('Button').at(0).simulate('click', null, { answer: ANSWERS[0] });
    expect(onAnswerMock).toHaveBeenCalledWith(ANSWERS[0]);
});
