const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const contextMock = require('autoru-frontend/mocks/contextMock').default;

const PersonalMessage = require('./PersonalMessage');

let props;

beforeEach(() => {
    props = {
        disabled: false,
        minimized: false,
        onClick: jest.fn(),
        onTooltipOpen: jest.fn(),
        type: PersonalMessage.TYPES.DEFAULT,
        hasChatOnlyFlag: false,
    };
});

it('если пришел флаг "только чат" правильно нарисует компонент', () => {
    props.hasChatOnlyFlag = true;
    props.type = PersonalMessage.TYPES.BUTTON;

    const wrapper = shallow(
        <PersonalMessage { ...props }/>,
        { context: contextMock },
    );

    expect(shallowToJson(wrapper)).toMatchSnapshot();
});

it('правильно нарисует компонент без флага "только чат"', () => {
    props.hasChatOnlyFlag = false;
    props.type = PersonalMessage.TYPES.BUTTON;

    const wrapper = shallow(
        <PersonalMessage { ...props }/>,
        { context: contextMock },
    );

    expect(shallowToJson(wrapper)).toMatchSnapshot();
});
