const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const ColorSelectFilter = require('./ColorSelectFilter');

it('не должен отрендериться, если список возможных цветов пуст', () => {
    const tree = shallow(
        <ColorSelectFilter
            items={ [] }
        />,
    );
    expect(shallowToJson(tree)).toEqual('');
});
