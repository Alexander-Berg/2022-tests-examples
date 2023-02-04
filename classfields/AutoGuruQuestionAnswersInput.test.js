const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const AutoGuruQuestionAnswersInput = require('./AutoGuruQuestionAnswersInput');
const onAnswerMock = jest.fn();

const ANSWERS = [
    {
        answer: 'От',
        search_param: 'price_from',
        default_value: '10000',
    },
    {
        answer: 'До',
        search_param: 'price_to',
        default_value: '300000000',
    },
];

let tree;
beforeEach(() => {
    tree = shallow(
        <AutoGuruQuestionAnswersInput
            answers={ ANSWERS }
            onAnswer={ onAnswerMock }
        />,
    );
});

it('должен отрендерить поля для пользовательского ввода значений ответов', () => {
    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('должен передать ответы с установленными пользователем значениями параметров', () => {
    tree.find('TextInput').at(0).simulate('change', '2000000', { paramName: 'price_from' });
    tree.find('TextInput').at(1).simulate('change', '2500000', { paramName: 'price_to' });
    tree.find('Button').simulate('click');
    expect(onAnswerMock).toHaveBeenCalledWith([
        {
            answer: 'От',
            search_param: 'price_from',
            default_value: '10000',
            price_from: '2000000',
        },
        {
            answer: 'До',
            search_param: 'price_to',
            default_value: '300000000',
            price_to: '2500000',
        },
    ]);
});
