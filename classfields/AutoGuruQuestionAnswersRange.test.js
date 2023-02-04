const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const RangeSlider = require('auto-core/react/components/desktop/RangeSlider/RangeSlider');
const AutoGuruQuestionAnswersRange = require('./AutoGuruQuestionAnswersRange');
const onAnswerMock = jest.fn();

const ANSWERS = [
    {
        answer: 'От',
        search_param: 'price_from',
        default_value: '1000000',
    },
    {
        answer: 'До',
        search_param: 'price_to',
        default_value: '3000000',
    },
];

let tree;
beforeEach(() => {
    tree = shallow(
        <AutoGuruQuestionAnswersRange
            answers={ ANSWERS }
            onAnswer={ onAnswerMock }
            unit="₽"
        />,
    );
});

it('должен отрендерить диапазон ответов в виде слайдера', () => {
    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('должен передать ответы с установленными пользователем значениями параметров', () => {
    tree.find(RangeSlider).at(0).simulate('change', { from: 2000000, to: 2500000 });
    tree.find('Button').simulate('click');
    expect(onAnswerMock).toHaveBeenCalledWith([
        {
            answer: 'От',
            search_param: 'price_from',
            default_value: '1000000',
            price_from: 2000000,
        },
        {
            answer: 'До',
            search_param: 'price_to',
            default_value: '3000000',
            price_to: 2500000,
        },
    ]);
});
