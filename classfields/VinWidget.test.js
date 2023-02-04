const React = require('react');

const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const contextMock = require('autoru-frontend/mocks/contextMock').default;
const mockStore = require('autoru-frontend/mocks/mockStore').default;

const VinWidget = require('./VinWidget');

const store = mockStore({
    config: {
        data: {
            pageParams: {
                theme: 'bmwClub',
            },
        },
    },
});

it('должен корректно отрендерить форму с меткой from', () => {
    const widget = shallow(
        <VinWidget store={ store }/>,
        { context: contextMock },
    ).dive();

    widget.find('TextInput').simulate('change', 'c022Еo99');

    expect(shallowToJson(widget)).toMatchSnapshot();
});

it('должен правильно обработать фигню вместо вина и госномера', () => {
    const widget = shallow(
        <VinWidget store={ store }/>,
        { context: contextMock },
    ).dive();

    widget.find('TextInput').simulate('change', 'продам гараж');

    expect(widget.state('linkParam')).toBeUndefined();
    expect(widget.find('.VinWidget__button').prop('url')).toEqual('link/proauto-report/?history_entity_id=&from=widget.vin.bmwclub');
});
