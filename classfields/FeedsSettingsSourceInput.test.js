const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const _ = require('lodash');

const { getBunkerMock } = require('autoru-frontend/mockData/state/bunker.mock');

const FeedsSettingsSourceInput = require('./FeedsSettingsSourceInput');

const bunkerNode = getBunkerMock([ 'cabinet/feeds' ])['cabinet/feeds'];

const baseProps = {
    settingsBunker: bunkerNode.settings,
    onChange: _.noop,
    shouldShowPopup: true,
};

it('должен показывать попап при фокусе на инпут', () => {
    const tree = shallow(
        <FeedsSettingsSourceInput
            { ...baseProps }
        />,
    );

    tree.find('TextInput').simulate('focusChange', true);

    expect(shallowToJson(tree)).toMatchSnapshot();
});
