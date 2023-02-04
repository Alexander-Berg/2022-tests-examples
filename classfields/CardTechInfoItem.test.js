const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const CardTechInfoItem = require('./CardTechInfoItem');

it('должен корректно отрендерить элемент технических характеристик', () => {
    const tree = shallow(
        <CardTechInfoItem
            name="Мощность"
            value="110"
            units="л.с"
            wrapperClassName="wrapperClassName"
            itemClassName="itemClassName"
            valueClassName="valueClassName"
        />,
    );
    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('должен содержать переданные классы', () => {
    const tree = shallow(
        <CardTechInfoItem
            name="Мощность"
            value="110"
            units="л.с"
            wrapperClassName="wrapperClassName"
            itemClassName="itemClassName"
            valueClassName="valueClassName"
        />,
    );
    expect(tree.find('.wrapperClassName')).toHaveLength(1);
    expect(tree.find('.itemClassName')).toHaveLength(1);
    expect(tree.find('.valueClassName')).toHaveLength(1);
});

it('не должен отрендерить элемент технических характеристик при нулевом value', () => {
    const tree = shallow(
        <CardTechInfoItem
            name="Количество цилиндров"
            value="0"
            units="шт"
            wrapperClassName="wrapperClassName"
            itemClassName="itemClassName"
            valueClassName="valueClassName"
        />,
    );
    expect(shallowToJson(tree)).toEqual('');
});
