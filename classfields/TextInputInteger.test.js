const React = require('react');
const { shallow } = require('enzyme');

const TextInputInteger = require('./TextInputInteger');

let props;

beforeEach(() => {
    props = {};
});

it('форматирует введенный текст', () => {
    props.value = 11222;
    const page = shallowRenderComponent({ props });
    const textInput = page.find('TextInput');

    expect(textInput.prop('value')).toBe('11 222');
});

describe('если есть суффикс', () => {
    beforeEach(() => {
        props.valueBlurSuffix = '₽';
    });

    it('добавит его если инпут не в фокусе', () => {
        props.value = 11222;
        const page = shallowRenderComponent({ props });
        const textInput = page.find('TextInput');

        expect(textInput.prop('value')).toBe('11 222 ₽');
    });

    it('не добавит его если инпут в фокусе', () => {
        props.value = 11222;
        const page = shallowRenderComponent({ props });
        const textInput = page.find('TextInput');
        textInput.simulate('focusChange', true);
        const updatedTextInput = page.find('TextInput');

        expect(updatedTextInput.prop('value')).toBe('11 222');
    });
});

describe('если в поле остались одни нули', () => {
    let page;

    beforeEach(() => {
        props.value = 1000000;
        props.onChange = jest.fn();
        page = shallowRenderComponent({ props });
        const textInput = page.find('TextInput');
        page.setProps({ ...props, value: 0 });
        textInput.simulate('change', '000000', { foo: 'bar' });
    });

    it('передаст в onChange ноль в качестве значения', () => {
        expect(props.onChange).toHaveBeenCalledTimes(1);
        expect(props.onChange).toHaveBeenCalledWith(0, { foo: 'bar' });
    });

    it('оставит в инпуте "000 000"', () => {
        const updatedTextInput = page.find('TextInput');

        expect(updatedTextInput.prop('value')).toBe('000 000');
    });
});

function shallowRenderComponent({ props }) {
    return shallow(<TextInputInteger { ...props }/>);
}
