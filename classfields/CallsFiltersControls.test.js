const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const _ = require('lodash');

const CallsFiltersControls = require('./CallsFiltersControls');

const filtersMock = require('www-cabinet/react/dataDomain/calls/mocks/withFilters.mock').filters;

const baseProps = {
    canExpand: false,
    onToggle: _.noop,
    onReset: _.noop,
};

it('должен рендерить только кнопку ресета, если нельзя расхлопнуть фильтры', () => {
    const tree = shallow(
        <CallsFiltersControls
            { ...baseProps }
            filters={ filtersMock }
        />,
    );

    expect(shallowToJson(tree)).toMatchSnapshot();
});
