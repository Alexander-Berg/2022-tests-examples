const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const ColorSelectorItem = require('./ColorSelectorItem');

const TEST_COLORS = [ 'FFFFFF', '000000' ];

it('должен корректно отрендериться', () => {
    const tree = shallow(
        <ColorSelectorItem
            colors={ TEST_COLORS }
            colorType="NOT_METALLIC"
        />,
    );
    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('не должен содержать класс selected, если selected не передан или false', () => {
    const tree = shallow(
        <ColorSelectorItem
            colors={ TEST_COLORS }
            colorType="NOT_METALLIC"
        />,
    );
    expect(tree.find('.ColorSelectorItem__selected')).toHaveLength(0);
});

it('должен содержать класс selected, если selected === true', () => {
    const tree = shallow(
        <ColorSelectorItem
            colors={ TEST_COLORS }
            colorType="NOT_METALLIC"
            selected={ true }
        />,
    );
    expect(tree.find('.ColorSelectorItem__selected')).toHaveLength(1);
});

it('должен содержать переданный класс', () => {
    const CLASS_NAME = 'Test_class_name';
    const tree = shallow(
        <ColorSelectorItem
            colors={ TEST_COLORS }
            colorType="NOT_METALLIC"
            className={ CLASS_NAME }
        />,
    );
    expect(tree.find('.' + CLASS_NAME)).toHaveLength(1);
});
