const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const BanMessage = require('./BanMessageDumb');

it('должен правильно отрендерить причины бана', () => {
    const wrapper = shallow(
        <BanMessage
            bunker={{
                foo: {
                    text_user_ban: 'foo ban text',
                },
                bar: {
                    text_user_ban: 'bar ban text',
                },
            }}
            className="Sales__banned"
            reasons={ [ 'foo', 'bar' ] }
        />);
    expect(shallowToJson(wrapper)).toMatchSnapshot();
});

it('должен правильно обрабатывать отсутствие причин бана', () => {
    const wrapper = shallow(
        <BanMessage
            bunker={{}}
            className="Sales__banned"
        />);
    expect(shallowToJson(wrapper)).toMatchSnapshot();
    wrapper.setProps({ reasons: [] });
    expect(shallowToJson(wrapper)).toMatchSnapshot();
});
