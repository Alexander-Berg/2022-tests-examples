const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const _ = require('lodash');

const CardGroupFilterReset = require('./CardGroupFilterReset');

it('должен отрендерить пустой див, если нет выбранных фильтров', () => {
    const tree = shallow(
        <CardGroupFilterReset
            onReset={ _.noop }
        />,
    );
    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('должен отрендерить кнопку сброса фильтров, если есть фильтры', () => {
    const tree = shallow(
        <CardGroupFilterReset
            visible={ true }
            onReset={ _.noop }
        />,
    );
    expect(shallowToJson(tree)).toMatchSnapshot();
});
