/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

const React = require('react');
const { shallow } = require('enzyme');

const contextMock = require('autoru-frontend/mocks/contextMock').default;

const MarkFilter = require('./MarkFilter');

const items = [
    { id: 'MARK1', name: 'mark1' },
    { id: 'MARK2', name: 'mark2' },
];

const onChange = jest.fn();

it('должен прокидывать значение исключения в селект', () => {
    const wrapper = shallow(
        <MarkFilter
            exclude
            items={ items }
            onChange={ onChange }
            value="MARK1"
            withExclude
            withVendors
        />, { context: contextMock },
    );
    expect(wrapper.dive().find('Menu').dive().find('.MarkFilter__mode RadioGroup').props().value).toBe(true);
    wrapper.setProps({ exclude: undefined });
    expect(wrapper.dive().find('Menu').dive().find('.MarkFilter__mode RadioGroup').props().value).toBe(false);
});

it('должен правильно обрабатывать изменение исключения', () => {
    const wrapper = shallow(
        <MarkFilter
            exclude
            items={ items }
            onChange={ onChange }
            value="MARK1"
            withExclude
            withVendors
        />, { context: contextMock },
    );
    wrapper.dive().find('Menu').dive().find('.MarkFilter__mode RadioGroup').simulate('change', false);
    expect(onChange).toHaveBeenCalledWith(false, { name: 'exclude' });
});

it('должен передавать в селект правильный плейсхолдер, если выбрана марка', () => {
    const wrapper = shallow(
        <MarkFilter
            items={ items }
            onChange={ onChange }
            value="MARK1"
            withExclude
            withVendors
        />, { context: contextMock },
    );
    expect(wrapper.find('Select').props().placeholder).toBe('mark1');
});

it('должен передавать в селект правильный плейсхолдер, если выбран вендор', () => {
    const wrapper = shallow(
        <MarkFilter
            items={ items }
            onChange={ onChange }
            value="VENDOR1"
            withExclude
            withVendors
        />, { context: contextMock },
    );
    expect(wrapper.find('Select').props().placeholder).toBe('Отечественные');
});

it('должен передавать в селект правильный плейсхолдер, если ничего не выбрано', () => {
    const wrapper = shallow(
        <MarkFilter
            items={ items }
            onChange={ onChange }
            withExclude
            withVendors
        />, { context: contextMock },
    );
    expect(wrapper.find('Select').props().placeholder).toBe('Марка');
});

it('должен отправить метрику на клик в кнопку "исключить"', () => {
    const wrapper = shallow(
        <MarkFilter
            items={ items }
            onChange={ onChange }
            withExclude
            withVendors
        />, { context: contextMock },
    );

    const modeButtons = wrapper.dive().find('Menu').dive().find('.MarkFilter__mode RadioGroup');
    modeButtons.simulate('change', true);
    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'mark-filter', 'exclude' ]);
});

it('должен отправить метрику на клик в кнопку "выбрать"', () => {
    const wrapper = shallow(
        <MarkFilter
            items={ items }
            onChange={ onChange }
            withExclude
            withVendors
        />, { context: contextMock },
    );

    const modeButtons = wrapper.dive().find('Menu').dive().find('.MarkFilter__mode RadioGroup');
    modeButtons.simulate('change', false);
    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'mark-filter', 'add' ]);
});
