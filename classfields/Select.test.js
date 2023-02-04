const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const Group = require('../Group');
const Item = require('../Item');
const ItemGroup = require('../ItemGroup');
const ItemGroupRoot = require('../ItemGroupRoot');
const Select = require('../Select');

it('Должен быть класс checked, если есть value', () => {
    const onChange = jest.fn();

    const wrapper = shallow(
        <Select
            mode={ Select.MODE.RADIO_CHECK }
            onChange={ onChange }
            value="fresh_relevance_1-desc"
        >
            <Item value="price-asc">По возрастанию цены</Item>
            <Item value="price-desc">По убыванию цены</Item>
        </Select>,
    );
    const className = wrapper.find('div').props().className;
    expect(className).toContain('Select_checked');
});

it('не должно быть класса checked, если value === дефолт', () => {
    const onChange = jest.fn();

    const wrapper = shallow(
        <Select
            clearItemText="По Актуальности"
            clearItemValue="fresh_relevance_1-desc"
            mode={ Select.MODE.RADIO_CHECK }
            onChange={ onChange }
            value="fresh_relevance_1-desc"
        >
            <Item value="price-asc">По возрастанию цены</Item>
            <Item value="price-desc">По убыванию цены</Item>
        </Select>,
    );
    const className = wrapper.find('div').props().className;
    expect(className).not.toContain('Select_checked');
});

it('должен быть класс checked, если value !== дефолт', () => {
    const onChange = jest.fn();

    const wrapper = shallow(
        <Select
            clearItemText="По Актуальности"
            clearItemValue="some_value"
            mode={ Select.MODE.RADIO_CHECK }
            onChange={ onChange }
            value="fresh_relevance_1-desc"
        >
            <Item value="price-asc">По возрастанию цены</Item>
            <Item value="price-desc">По убыванию цены</Item>
        </Select>,
    );
    const className = wrapper.find('div').props().className;
    expect(className).toContain('Select_checked');
});

it('должен вызвать onChange с пустым значением при сбросе', () => {
    const onChange = jest.fn();

    const wrapper = shallow(
        <Select
            clearItemText="По Актуальности"
            mode={ Select.MODE.RADIO_CHECK }
            onChange={ onChange }
            value="fresh_relevance_1-desc"
        >
            <Item value="price-asc">По возрастанию цены</Item>
            <Item value="price-desc">По убыванию цены</Item>
        </Select>,
    );
    const clearValue = wrapper.find('.MenuItem_has-clear').props().value;
    wrapper.find('Menu').simulate('change', clearValue);
    expect(onChange.mock.calls[0][0]).toEqual([]);
});

it('должен вызвать onChange с clearItemValue при сбросе', () => {
    const onChange = jest.fn();

    const wrapper = shallow(
        <Select
            clearItemText="По Актуальности"
            clearItemValue="some_value"
            mode={ Select.MODE.RADIO_CHECK }
            onChange={ onChange }
            value="fresh_relevance_1-desc"
        >
            <Item value="price-asc">По возрастанию цены</Item>
            <Item value="price-desc">По убыванию цены</Item>
        </Select>,
    );
    const clearValue = wrapper.find('.MenuItem_has-clear').props().value;
    wrapper.find('Menu').simulate('change', clearValue);
    expect(onChange.mock.calls[0][0]).toEqual('some_value');
});

it('должен правильно фильровать детишек при поиске', () => {
    const children = (
        [
            <Group key="popular">
                <ItemGroup>
                    <ItemGroupRoot value="C-klasse">C-klasse</ItemGroupRoot>
                    <Item value="C-klasse#1">1</Item>
                    <Item value="C-klasse#2">2</Item>
                </ItemGroup>
                <Item value="C-klasse-1">C-klasse 1</Item>
                <Item value="E-klasse">E-klasse</Item>
            </Group>,
            <Group key="all">
                <ItemGroup>
                    <ItemGroupRoot value="C-klasse">C-klasse</ItemGroupRoot>
                    <Item value="C-klasse#1">1</Item>
                    <Item value="C-klasse#2">2</Item>
                </ItemGroup>
                <ItemGroup>
                    <ItemGroupRoot value="C-klasse-AMG">C-klasse AMG</ItemGroupRoot>
                    <Item value="C-klasse-AMG#1">1 AMG</Item>
                    <Item value="C-klasse-AMG#2">2 AMG</Item>
                </ItemGroup>
                <Item value="C-klasse-1">C-klasse 1</Item>
                <Item value="E-klasse">E-klasse</Item>
                <Item value="E-klasse-AMG">E-klasse AMG</Item>
            </Group>,
        ]
    );
    const onChange = jest.fn();

    const wrapper = shallow(
        <Select
            mode={ Select.MODE.CHECK }
            onChange={ onChange }
            value={ [] }
            withSearch
        >
            { children }
        </Select>,
    );
    wrapper.setState({ popupVisible: true, searchInputValue: 'AMG' });
    expect(shallowToJson(wrapper.find('Menu').children())).toMatchSnapshot();
});
