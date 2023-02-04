const React = require('react');
const { shallow } = require('enzyme');
const _ = require('lodash');

const { nbsp } = require('auto-core/react/lib/html-entities');

const contextMock = require('autoru-frontend/mocks/contextMock').default;
const complectationFilterData = require('auto-core/react/dataDomain/cardGroup/mocks/complectationFilterData.mock');

const CardGroupFilterComplectation = require('./CardGroupFilterComplectation');

const NBSP_REGEXP = new RegExp(nbsp, 'g');

const baseProps = {
    items: complectationFilterData.COMPLECTATION_ITEMS,
    additionalOptions: complectationFilterData.ADDITIONAL_OPTIONS,
    additionalOptionsGrouped: complectationFilterData.ADDITIONAL_OPTIONS_GROUPED,
    availableOptionsGrouped: complectationFilterData.AVAILABLE_OPTIONS_GROUPED,
    offersCount: 10,
    onChangeComplectation: _.noop,
    onChangeOptions: _.noop,
    onReset: _.noop,
    onSubmit: _.noop,
};

it('должен отобразить текст "Комплектация и опции", если ни одна комплектация не выбрана', () => {
    const tree = shallow(
        <CardGroupFilterComplectation
            { ...baseProps }
            selectedComplectationsNames={ [] }
            selectedOptions={ [] }
        />,
        { context: contextMock },
    );
    const placeholder = tree.find('Select').at(0).prop('placeholder');
    expect(placeholder).toEqual('Комплектация и опции');
});

it('должен отобразить текст "Все комплектации" и количество выбранных доп. опций', () => {
    const tree = shallow(
        <CardGroupFilterComplectation
            { ...baseProps }
            selectedComplectationsNames={ [] }
            selectedOptions={ [ 'option1', 'option2,option3' ] }
        />,
        { context: contextMock },
    );
    const placeholder = tree.find('Select').at(0).prop('placeholder').replace(NBSP_REGEXP, ' ');
    expect(placeholder).toEqual('Все комплектации + 3 опции');
});

it('должен отобразить название выбранной комплектации', () => {
    const tree = shallow(
        <CardGroupFilterComplectation
            { ...baseProps }
            selectedComplectationsNames={ [ 'Комплектация 1' ] }
            selectedOptions={ [] }
        />,
        { context: contextMock },
    );
    const placeholder = tree.find('Select').at(0).prop('placeholder');
    expect(placeholder).toEqual('Комплектация 1');
});

it('должен отобразить название выбранной комплектации и количество выбранных доп. опций', () => {
    const tree = shallow(
        <CardGroupFilterComplectation
            { ...baseProps }
            selectedComplectationsNames={ [ 'Комплектация 1' ] }
            selectedOptions={ [ 'option1', 'option2' ] }
        />,
        { context: contextMock },
    );
    const placeholder = tree.find('Select').at(0).prop('placeholder').replace(NBSP_REGEXP, ' ');
    expect(placeholder).toEqual('Комплектация 1 + 2 опции');
});

it('должен отобразить название выбранной комплектации и количество выбранных доп. опций если выбраны еще и основные опции', () => {
    const tree = shallow(
        <CardGroupFilterComplectation
            { ...baseProps }
            selectedComplectationsNames={ [ 'Комплектация 1' ] }
            selectedOptions={ [ 'option1', 'option2,option_1', 'option_2' ] }
        />,
        { context: contextMock },
    );
    const placeholder = tree.find('Select').at(0).prop('placeholder').replace(NBSP_REGEXP, ' ');
    expect(placeholder).toEqual('Комплектация 1 + 2 опции');
});

it('при клике на комплектацию должен передать ее имя в onChange', () => {
    const onChangeMock = jest.fn();

    const tree = shallow(
        <CardGroupFilterComplectation
            { ...baseProps }
            onChangeComplectation={ onChangeMock }
        />,
        { context: contextMock },
    );

    const item = tree.find({ value: 'Комплектация 1' }).at(0).dive().find('.CardGroupFilterComplectationItem');
    item.simulate('click');

    expect(onChangeMock).toHaveBeenCalledWith('Комплектация 1');
});
