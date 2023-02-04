/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

const React = require('react');
const { shallow } = require('enzyme');

const AddressSuggest = require('./AddressSuggest');
const RichInput = require('auto-core/react/components/common/RichInput/RichInput').default;

it('должен вызвать super.onInputChange с корректными параметрами', () => {
    RichInput.prototype.onInputChange = jest.fn();
    const wrapper = shallow(<AddressSuggest/>);
    wrapper.find('.RichInput__input-control').simulate('change', { target: { value: 'Балашиха' } });

    expect(RichInput.prototype.onInputChange).toHaveBeenCalledWith('Балашиха');
});

it('должен вызвать onInputChange с корректными параметрами, если передан props.onInputChange', () => {
    const onInputChange = jest.fn();
    const wrapper = shallow(<AddressSuggest onInputChange={ onInputChange }/>);
    wrapper.find('.RichInput__input-control').simulate('change', { target: { value: 'Балашиха' } });

    expect(onInputChange).toHaveBeenCalledWith('Балашиха');
});
