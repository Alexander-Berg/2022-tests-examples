/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import React from 'react';
import { renderHook, act } from '@testing-library/react-hooks';
import type { PropsWithChildren } from 'react';

import sleep from 'auto-core/lib/sleep';

import type { FormContext } from 'auto-core/react/components/common/Form/types';
import { FormProvider } from 'auto-core/react/components/common/Form/contexts/FormContext';

import useFormField from './useFormField';

enum ErrorTypes {
    REQUIRED = 'required',
    LOL = 'lol',
}

enum FieldNames {
    FIELD_WITH_GROUP = 'group.field',
    FIELD = 'field',
    DEPENDENT_FIELD = 'dependent_field',
}

type Fields = {
    [ FieldNames.FIELD_WITH_GROUP ]: string;
    [ FieldNames.FIELD ]: number;
    [ FieldNames.DEPENDENT_FIELD ]: number;
}

const PropsMock = {
    name: FieldNames.FIELD_WITH_GROUP,
    dependentFields: [ FieldNames.DEPENDENT_FIELD ],
    validator: jest.fn(),
    onChange: jest.fn(),
    onFocus: jest.fn(),
    onBlur: jest.fn(),
    valueProcessor: jest.fn((any) => 'processed ' + any),
};

const FormContextMock = {
    blurField: jest.fn(),
    focusField: jest.fn(),
    getFieldError: jest.fn(),
    getFieldValue: jest.fn(),
    handleFieldChange: jest.fn(),
    isFieldFocused: jest.fn(),
    isFieldTouched: jest.fn(),
    registerField: jest.fn(),
    unregisterField: jest.fn(),
    validateField: jest.fn(),
};

const ContextProviderComponent = (props: PropsWithChildren<unknown>) => {
    return (
        <FormProvider value={ FormContextMock as unknown as FormContext<FieldNames, Fields, ErrorTypes> }>
            { props.children }
        </FormProvider>
    );
};

it('прокидывает правильные значения из формы', () => {
    FormContextMock.getFieldValue.mockImplementationOnce(() => 'value');
    FormContextMock.getFieldError.mockImplementationOnce(() => ({
        text: 'error',
        type: ErrorTypes.REQUIRED,
    }));
    FormContextMock.isFieldFocused.mockImplementationOnce(() => false);
    FormContextMock.isFieldTouched.mockImplementationOnce(() => false);

    const {
        result,
    } = renderHook(props => useFormField<FieldNames, Fields, ErrorTypes>(props), {
        initialProps: PropsMock,
        wrapper: ContextProviderComponent,
    });

    expect(result.current.value).toEqual('value');
    expect(result.current.touched).toEqual(false);
    expect(result.current.focused).toEqual(false);
    expect(result.current.error).toEqual({
        text: 'error',
        type: ErrorTypes.REQUIRED,
    });
});

it('регистрирует поле', () => {
    renderHook(props => useFormField<FieldNames, Fields, ErrorTypes>(props), {
        initialProps: PropsMock,
        wrapper: ContextProviderComponent,
    });

    expect(FormContextMock.registerField).toHaveBeenCalledWith({
        name: PropsMock.name,
        dependentFields: PropsMock.dependentFields,
        validator: PropsMock.validator,
    });
});

it('на анмаунте удаляет регистрацию поля', () => {
    const {
        unmount,
    } = renderHook(props => useFormField<FieldNames, Fields, ErrorTypes>(props), {
        initialProps: PropsMock,
        wrapper: ContextProviderComponent,
    });

    act(() => {
        unmount();
    });

    expect(FormContextMock.unregisterField).toHaveBeenCalledWith(PropsMock.name);
});

it('setValue передает в форму и onChange новые значения (с valueProcessor)', () => {
    const {
        result,
    } = renderHook(props => useFormField<FieldNames, Fields, ErrorTypes>(props), {
        initialProps: PropsMock,
        wrapper: ContextProviderComponent,
    });

    act(() => {
        result.current.setValue('lol');
    });

    expect(FormContextMock.handleFieldChange).toHaveBeenCalledWith(PropsMock.name, 'processed lol');
    expect(PropsMock.valueProcessor).toHaveBeenCalledWith('lol');
    expect(PropsMock.onChange).toHaveBeenCalledWith('processed lol');
});

it('setValue передает в форму и onChange новые значения (с valueProcessor и значением undefined)', () => {
    const {
        result,
    } = renderHook(props => useFormField<FieldNames, Fields, ErrorTypes>(props), {
        initialProps: PropsMock,
        wrapper: ContextProviderComponent,
    });

    act(() => {
        result.current.setValue(undefined);
    });

    expect(FormContextMock.handleFieldChange).toHaveBeenCalledWith(PropsMock.name, undefined);
    expect(PropsMock.valueProcessor).not.toHaveBeenCalled();
    expect(PropsMock.onChange).toHaveBeenCalledWith(undefined);
});

it('setValue передает в форму и onChange новые значения (без valueProcessor)', () => {
    const {
        result,
    } = renderHook(props => useFormField<FieldNames, Fields, ErrorTypes>(props), {
        initialProps: {
            ...PropsMock,
            valueProcessor: undefined,
        },
        wrapper: ContextProviderComponent,
    });

    act(() => {
        result.current.setValue('lol');
    });

    expect(FormContextMock.handleFieldChange).toHaveBeenCalledWith(PropsMock.name, 'lol');
    expect(PropsMock.onChange).toHaveBeenCalledWith('lol');
});

it('handleChangeEvent берет из эвента значение и передает в установку', () => {
    const {
        result,
    } = renderHook(props => useFormField<FieldNames, Fields, ErrorTypes>(props), {
        initialProps: PropsMock,
        wrapper: ContextProviderComponent,
    });

    act(() => {
        result.current.handleChangeEvent({
            target: {
                value: 'lol',
            },
        } as React.ChangeEvent<HTMLInputElement>);
    });

    expect(FormContextMock.handleFieldChange).toHaveBeenCalledWith(PropsMock.name, 'processed lol');
    expect(PropsMock.valueProcessor).toHaveBeenCalledWith('lol');
    expect(PropsMock.onChange).toHaveBeenCalledWith('processed lol');
});

it('handleBlur запускает blurField и отложено запускает валидацию', async() => {
    const {
        result,
    } = renderHook(props => useFormField<FieldNames, Fields, ErrorTypes>(props), {
        initialProps: PropsMock,
        wrapper: ContextProviderComponent,
    });

    await act(async() => {
        result.current.handleBlur();

        await sleep(110);
    });

    expect(FormContextMock.blurField).toHaveBeenCalledWith(PropsMock.name);
    expect(PropsMock.onBlur).toHaveBeenCalledTimes(1);
    expect(FormContextMock.blurField).toHaveBeenCalledTimes(1);
    expect(FormContextMock.validateField).toHaveBeenCalledWith(PropsMock.name);
    expect(FormContextMock.validateField).toHaveBeenCalledTimes(1);
});

it('handleFocus запускает focusField', async() => {
    const {
        result,
    } = renderHook(props => useFormField<FieldNames, Fields, ErrorTypes>(props), {
        initialProps: PropsMock,
        wrapper: ContextProviderComponent,
    });

    await act(async() => {
        result.current.handleFocus({} as React.SyntheticEvent<Element, Event>);
    });

    expect(FormContextMock.focusField).toHaveBeenCalledWith(PropsMock.name);
    expect(FormContextMock.focusField).toHaveBeenCalledTimes(1);
    expect(PropsMock.onFocus).toHaveBeenCalledTimes(1);
});

it('при вызове handleBlur и сразу handleFocus - validateField не запускается', async() => {
    const {
        result,
    } = renderHook(props => useFormField<FieldNames, Fields, ErrorTypes>(props), {
        initialProps: PropsMock,
        wrapper: ContextProviderComponent,
    });

    await act(async() => {
        result.current.handleBlur();
        result.current.handleFocus({} as React.SyntheticEvent<Element, Event>);

        await sleep(100);
    });

    expect(PropsMock.onFocus).toHaveBeenCalledTimes(1);
    expect(PropsMock.onBlur).toHaveBeenCalledTimes(1);
    expect(FormContextMock.focusField).toHaveBeenCalledTimes(1);
    expect(FormContextMock.blurField).toHaveBeenCalledTimes(1);
    expect(FormContextMock.validateField).not.toHaveBeenCalled();
});

it('при вызове handleBlur и handleFocusChange(false) - validateField вызывается один раз', async() => {
    const {
        result,
    } = renderHook(props => useFormField<FieldNames, Fields, ErrorTypes>(props), {
        initialProps: PropsMock,
        wrapper: ContextProviderComponent,
    });

    await act(async() => {
        result.current.handleBlur();
        result.current.handleFocusChange(false);

        await sleep(100);
    });

    expect(FormContextMock.validateField).toHaveBeenCalledTimes(1);
});
