const _ = require('lodash');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const React = require('react');

const PhoneInput = require('./PhoneInput');
const INITIAL_PHONE_VALUE = '+7';

let props;
const defaultProps = {
    className: 'Foo',
    disabled: false,
    hasSuggest: true,
    hasInitialFocus: false,
    initialValue: '79981234567',
    size: PhoneInput.SIZE.L,
    suggest: [ '79981234567', '79881234567', '79891234567' ],
    onChange: jest.fn(),
    onReset: jest.fn(),
    onSubmit: jest.fn(),
};

const submitEventMock = {
    preventDefault: () => {},
};

beforeEach(() => {
    props = _.cloneDeep(defaultProps);
});

it('правильно рисует компонент без саджеста', () => {
    props.hasSuggest = false;
    const page = shallowRenderComponent({ props });
    expect(shallowToJson(page)).toMatchSnapshot();
});

it('правильно рисует компонент с саджестом', () => {
    props.hasSuggest = true;
    const page = shallowRenderComponent({ props });
    expect(shallowToJson(page)).toMatchSnapshot();
});

describe('поставит дефолтное значение если начальное не передано', () => {
    it('для компонента без саджеста', () => {
        props.hasSuggest = false;
        props.initialValue = undefined;
        const page = shallowRenderComponent({ props });
        const inputComponent = page.find('TextInput');

        expect(inputComponent.prop('value')).toBe(INITIAL_PHONE_VALUE);
    });

    it('для компонента с саджестом', () => {
        props.hasSuggest = true;
        props.initialValue = undefined;
        const page = shallowRenderComponent({ props });
        const inputComponent = page.find('PhoneInputWithSuggest');

        expect(inputComponent.prop('initialValue')).toBe(INITIAL_PHONE_VALUE);
    });

    it('при фокусе на компонент дефолтное значение не поменяется', () => {
        props.hasSuggest = false;
        props.initialValue = undefined;
        const page = shallowRenderComponent({ props });
        const inputComponent = page.find('TextInput');
        inputComponent.simulate('focusChange', true);

        const updatedInputComponent = page.find('TextInput');

        expect(updatedInputComponent.prop('value')).toBe(INITIAL_PHONE_VALUE);
    });
});

describe('если передан флаг "showPlaceholderIfNoValue"', () => {
    let page;
    let inputComponent;

    beforeEach(() => {
        props.hasSuggest = false;
        props.initialValue = undefined;
        props.showPlaceholderIfNoValue = true;

        page = shallowRenderComponent({ props });
        inputComponent = page.find('TextInput');
    });

    it('поставит в инпут пустое значение при рендере', () => {
        expect(inputComponent.prop('value')).toBe('');
    });

    it('при фокусе если инпут был пустым подставит туда дефолтное значение +7', () => {
        inputComponent.simulate('focusChange', true);
        const updatedInputComponent = page.find('TextInput');

        expect(updatedInputComponent.prop('value')).toBe(INITIAL_PHONE_VALUE);
    });

    it('при фокусе если инпут не был пустым его значение не поменяется', () => {
        inputComponent.simulate('change', '+790935133');
        inputComponent.simulate('focusChange', true);

        const updatedInputComponent = page.find('TextInput');

        expect(updatedInputComponent.prop('value')).toBe('+7 909 351-33');
    });

    it('при расфокусе если инпут был пустым подставит туда пустое значение', () => {
        inputComponent.simulate('focusChange', true);
        inputComponent.simulate('focusChange', false);
        const updatedInputComponent = page.find('TextInput');

        expect(updatedInputComponent.prop('value')).toBe('');
    });

    it('при расфокусе если инпут  не был пустым его значение не поменяется', () => {
        inputComponent.simulate('change', '+790935133');
        inputComponent.simulate('focusChange', true);
        inputComponent.simulate('focusChange', false);
        const updatedInputComponent = page.find('TextInput');

        expect(updatedInputComponent.prop('value')).toBe('+7 909 351-33');
    });
});

describe('при изменении значения в поле', () => {
    it('если оно пустое поставит дефолтное значение', () => {
        props.hasSuggest = false;
        const page = shallowRenderComponent({ props });

        const input = page.find('TextInput');
        input.simulate('change', '');

        const updatedInput = page.find('TextInput');
        expect(updatedInput.prop('value')).toBe(INITIAL_PHONE_VALUE);
    });

    it('если оно равно "+" поставит дефолтное значение', () => {
        props.hasSuggest = false;
        const page = shallowRenderComponent({ props });

        const input = page.find('TextInput');
        input.simulate('change', '+');

        const updatedInput = page.find('TextInput');
        expect(updatedInput.prop('value')).toBe(INITIAL_PHONE_VALUE);
    });

    it('если оно не пустое отформатирует его', () => {
        props.hasSuggest = false;
        const page = shallowRenderComponent({ props });

        const input = page.find('TextInput');
        input.simulate('change', '+790935133');

        const updatedInput = page.find('TextInput');
        expect(updatedInput.prop('value')).toBe('+7 909 351-33');
    });

    it('если до этого была ошибка то сбросит её', () => {
        props.hasSuggest = false;
        const page = shallowRenderComponent({ props });
        page.instance().setState({ hasError: true });

        const input = page.find('TextInput');
        expect(input.prop('error')).not.toBe(false);
        expect(input.prop('error')).not.toBe('');

        input.simulate('change', '+790935133');
        const updatedInput = page.find('TextInput');
        expect(updatedInput.prop('error')).toBe(false);
    });

    it('если телефон введен полностью сообщит об этом', () => {
        let exposedProps;
        props.hasSuggest = false;
        props.children = (props) => exposedProps = props;
        const page = shallowRenderComponent({ props });

        const input = page.find('TextInput');
        input.simulate('change', '+79093513311');

        expect(exposedProps.hasFocusOnButton).toBe(true);
    });

    it('если есть проп onChange, будет передавать в него данные при изменении инпута', () => {
        props.hasSuggest = false;
        const page = shallowRenderComponent({ props });

        const input = page.find('TextInput');
        input.simulate('change', '+7909');

        expect(props.onChange).toHaveBeenLastCalledWith('7909', false);

        input.simulate('change', '+79091234567');
        expect(props.onChange).toHaveBeenLastCalledWith('79091234567', true);
    });

    it('не поменяет введенное значение, если передан новый initialValue и не вызовет повторно onChange', () => {
        props.hasSuggest = false;
        const phoneInput = shallowRenderComponent({ props });

        const input = phoneInput.find('TextInput');
        input.simulate('change', '+790912345');

        phoneInput.setProps({
            ...props,
            initialValue: '+79091234533',
        });
        phoneInput.instance().componentDidUpdate(props);

        expect(phoneInput.state().value).toEqual('+7 909 123-45');
        expect(props.onChange).toHaveBeenCalledWith('790912345', false);
    });

    it('если передан новый initialValue заменит старый initialValue и вызовет onChange', () => {
        props.hasSuggest = false;
        const phoneInput = shallowRenderComponent({ props });

        phoneInput.setProps({
            ...props,
            initialValue: '+79091234533',
        });
        phoneInput.instance().componentDidUpdate(props);

        expect(phoneInput.state().value).toEqual('+7 909 123-45-33');
        expect(props.onChange).toHaveBeenLastCalledWith('79091234533', true);
    });
});

describe('при сабмите', () => {
    describe('если телефон не корретный', () => {
        let page;
        let exposedProps;

        beforeEach(() => {
            props.hasSuggest = false;
            props.children = (props) => exposedProps = props;

            page = shallowRenderComponent({ props });
            const input = page.find('TextInput');
            input.simulate('change', '+790935133');

            exposedProps.onSubmit(submitEventMock);
        });

        it('покажет ошибку', () => {
            const updatedInput = page.find('TextInput');
            expect(updatedInput.prop('error')).toBe('Пожалуйста, введите корректный номер телефона');
        });

        it('не вызовет проп', () => {
            expect(props.onSubmit).toHaveBeenCalledTimes(0);
        });
    });

    describe('если телефон корретный', () => {
        let page;
        let exposedProps;

        beforeEach(() => {
            props.hasSuggest = false;
            props.children = (props) => exposedProps = props;

            page = shallowRenderComponent({ props });
            const input = page.find('TextInput');
            input.simulate('change', '+79093513311');

            exposedProps.onSubmit(submitEventMock);
        });

        it('передаст в проп номер телефона', () => {
            expect(props.onSubmit).toHaveBeenCalledTimes(1);
            expect(props.onSubmit).toHaveBeenCalledWith(submitEventMock, '79093513311');
        });
    });
});

describe('при клике на крест', () => {
    let page;
    let exposedProps;
    let input;

    beforeEach(() => {
        props.hasSuggest = false;
        props.children = (props) => exposedProps = props;

    });

    it('если телефон не был засабмичен, сбросит инпут', () => {
        page = shallowRenderComponent({ props });
        input = page.find('TextInput');
        input.simulate('change', '+79093513311');
        input.simulate('clearClick');

        const updatedInput = page.find('TextInput');
        expect(updatedInput.prop('value')).toBe(INITIAL_PHONE_VALUE);
        expect(updatedInput.prop('error')).toBe(false);
    });

    it('если инпут не был засабмичен и задисейблен, вызовет onChange и не будет сбрасывать инпут', () => {
        props.disabled = true;
        page = shallowRenderComponent({ props });
        input = page.find('TextInput');

        input.simulate('change', '+79093513311');
        expect(props.onChange).toHaveBeenLastCalledWith('79093513311', true);

        input.simulate('clearClick');
        const updatedInput = page.find('TextInput');
        expect(updatedInput.prop('value')).toBe('+7 909 351-33-11');

        expect(props.onChange).toHaveBeenLastCalledWith('79093513311', false);
    });

    it('если телефон был засабмичен и резет не передан, ничего не будет делать', () => {
        props.onReset = undefined;

        page = shallowRenderComponent({ props });
        input = page.find('TextInput');
        input.simulate('change', '+79093513311');
        exposedProps.onSubmit(submitEventMock);
        input.simulate('clearClick');

        const updatedInput = page.find('TextInput');
        expect(updatedInput.prop('value')).toBe('+7 909 351-33-11');
    });

    it('если телефон был засабмичен и резет передан, не сбросит инпут и вызовет резет', () => {
        page = shallowRenderComponent({ props });
        input = page.find('TextInput');
        input.simulate('change', '+79093513311');
        exposedProps.onSubmit(submitEventMock);
        input.simulate('clearClick');

        const updatedInput = page.find('TextInput');
        expect(updatedInput.prop('value')).toBe('+7 909 351-33-11');
        expect(updatedInput.prop('error')).toBe(false);
        expect(props.onReset).toHaveBeenCalledTimes(1);
    });
});

it('поставит фокус при рендере в инпут если у него нет начального значения и передан соответствующий флаг', () => {
    props.hasInitialFocus = true;
    props.initialValue = undefined;

    const page = shallowRenderComponent({ props });

    expect(page.instance().phoneInput.focus).toHaveBeenCalledTimes(1);
});

function shallowRenderComponent({ props }) {
    const page = shallow(<PhoneInput { ...props }/>, { disableLifecycleMethods: true });
    const instance = page.instance();
    instance.phoneInput = {
        focus: jest.fn(),
    };
    instance.componentDidMount();

    return page;
}
