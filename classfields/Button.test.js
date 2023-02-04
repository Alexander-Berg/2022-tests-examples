const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const Button = require('./Button');

it('должен отрендерить текст кнопки в <span class="Button__text"/>', () => {
    const wrapper = shallow(
        <Button>foo</Button>,
    );

    expect(shallowToJson(wrapper)).toMatchSnapshot();
});

it('должен отрендерить текст в <span class="Button__text"/>, а компонент в <span class="Button__content"/>', () => {
    const wrapper = shallow(
        <Button>
            foo
            <div>tag</div>
        </Button>,
    );

    expect(shallowToJson(wrapper)).toMatchSnapshot();
});

describe('компонент внутри кнопки должен отрендерить внутри <span class="Button__content"/> без <span class="Button__text"/>', () => {
    it('должен правильно отрендерить тег', () => {
        const wrapper = shallow(
            <Button><div>tag</div></Button>,
        );
        expect(shallowToJson(wrapper)).toMatchSnapshot();
    });
    it('должен правильно отрендерить stateless компонент', () => {
        const Foo = () => <div>stateless</div>;
        const wrapper = shallow(
            <Button><Foo/></Button>,
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
            <Button><Foo/></Button>,
        );
        expect(shallowToJson(wrapper)).toMatchSnapshot();
    });
});
