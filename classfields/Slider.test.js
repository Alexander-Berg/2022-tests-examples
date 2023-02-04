const React = require('react');
const { shallow } = require('enzyme');

const Slider = require('./Slider');

const defaultProps = {
    items: [ { value: 1 } ],
    toValue: 1,
};

it('поставит тоглер в крайнее правое положение, если в массиве значений только один элемент', () => {
    const tree = shallowRenderComponent();

    expect(tree.state('toPos')).toEqual(100);
});

function shallowRenderComponent(props = defaultProps) {
    return shallow(
        <Slider { ...props }/>,
    );
}
