const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const OptionsSelectFilter = require('./OptionsSelectFilter');

it('не должен отрендериться, если список возможных опций пуст', () => {
    const tree = shallow(
        <OptionsSelectFilter
            items={ [] }
        />,
    );
    expect(shallowToJson(tree)).toEqual('');
});
