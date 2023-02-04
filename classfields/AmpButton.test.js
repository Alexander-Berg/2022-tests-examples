const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const AmpButton = require('./AmpButton');

it('должен отрендерить текст кнопки в <span class="AmpButton__text"/>', () => {
    const wrapper = shallow(
        <AmpButton>foo</AmpButton>,
    );

    expect(shallowToJson(wrapper)).toMatchSnapshot();
});

it('должен отрендерить текст в <span class="AmpButton__text"/>, а компонент в <span class="AmpButton__content"/>', () => {
    const wrapper = shallow(
        <AmpButton>
            foo
            <div>tag</div>
        </AmpButton>,
    );

    expect(shallowToJson(wrapper)).toMatchSnapshot();
});

describe('компонент внутри кнопки должен отрендерить внутри <span class="AmpButton__content"/> без <span class="AmpButton__text"/>', () => {
    it('должен правильно отрендерить тег', () => {
        const wrapper = shallow(
            <AmpButton><div>tag</div></AmpButton>,
        );
        expect(shallowToJson(wrapper)).toMatchSnapshot();
    });
    it('должен правильно отрендерить stateless компонент', () => {
        const Foo = () => <div>stateless</div>;
        const wrapper = shallow(
            <AmpButton><Foo/></AmpButton>,
        );
        expect(shallowToJson(wrapper)).toMatchSnapshot();
    });
    it('должен правильно отрендерить компонент', () => {
        class Foo extends React.PureComponent {
            render() {
                return <div>component</div>;
            }
        }
        const wrapper = shallow(
            <AmpButton><Foo/></AmpButton>,
        );
        expect(shallowToJson(wrapper)).toMatchSnapshot();
    });
});
