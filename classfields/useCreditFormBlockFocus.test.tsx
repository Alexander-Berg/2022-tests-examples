/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import React from 'react';
import { renderHook, act } from '@testing-library/react-hooks';
import { mount } from 'enzyme';
import type {
    FormikProps,
    FormikValues,
} from 'formik';

import type { CreditFormFieldsRegistry } from 'auto-core/react/components/common/CreditForm/types';

import { creditFormFieldsRegistryFieldMockGenerator } from './creditFormFieldsRegistryField.mock';
import useCreditFormBlockFocus from './useCreditFormBlockFocus';
import type { UseCreditFormBlockFocusParams } from './useCreditFormBlockFocus';

const validateField: FormikProps<FormikValues>['validateField'] = jest.fn();
let values: FormikProps<FormikValues>['values'];
let errors: FormikProps<FormikValues>['errors'];
let touched: FormikProps<FormikValues>['touched'];
let fieldsRegistry: CreditFormFieldsRegistry;
let childrenRef: UseCreditFormBlockFocusParams<FormikValues>['childrenRef'];
const onFieldFocusChange: UseCreditFormBlockFocusParams<FormikValues>['onFieldFocusChange'] = jest.fn();

beforeEach(() => {
    values = {};
    errors = {};
    touched = {};
    fieldsRegistry = {
        'field-name-1': {
            ...creditFormFieldsRegistryFieldMockGenerator(),
            name: 'field-name-1',
            dependentFields: [ 'field-name-2', 'field-name-3' ],
        },
        'field-name-2': {
            ...creditFormFieldsRegistryFieldMockGenerator(),
            name: 'field-name-2',
        },
    };
    childrenRef = React.createRef();
});

it('focusField вызывает focus у нужного поля', () => {
    const props: UseCreditFormBlockFocusParams<FormikValues> = {
        fieldsRegistry,
        highlightFocusedField: true,
        childrenRef,
        onFieldFocusChange,
        validateField,
        values,
        errors,
        touched,
    };

    const {
        result,
    } = renderHook((props) => useCreditFormBlockFocus(props), {
        initialProps: props,
    });

    result.current.focusField('field-name-1');

    expect(fieldsRegistry['field-name-1'].focus).toHaveBeenCalledTimes(1);
    expect(fieldsRegistry['field-name-2'].focus).not.toHaveBeenCalledTimes(1);
});

it('blurField вызывает blur у нужного поля', () => {
    const props: UseCreditFormBlockFocusParams<FormikValues> = {
        fieldsRegistry,
        highlightFocusedField: true,
        childrenRef,
        onFieldFocusChange,
        validateField,
        values,
        errors,
        touched,
    };

    const {
        result,
    } = renderHook((props) => useCreditFormBlockFocus(props), {
        initialProps: props,
    });

    result.current.blurField('field-name-1');

    expect(fieldsRegistry['field-name-1'].blur).toHaveBeenCalledTimes(1);
    expect(fieldsRegistry['field-name-2'].blur).not.toHaveBeenCalledTimes(1);
});

describe('handleFieldFocus', () => {
    it('при фокусе вызывает highlight у фокусируемого поля и unhighlight у остальных с передачей placeToTopOnHighlight фокусируемого поля', () => {
        const props: UseCreditFormBlockFocusParams<FormikValues> = {
            fieldsRegistry: {
                ...fieldsRegistry,
                'field-name-1': {
                    ...fieldsRegistry['field-name-1'],
                    placeToTopOnHighlight: true,
                },
            },
            highlightFocusedField: true,
            childrenRef,
            onFieldFocusChange,
            validateField,
            values,
            errors,
            touched,
        };

        const {
            result,
        } = renderHook((props) => useCreditFormBlockFocus(props), {
            initialProps: props,
        });

        result.current.handleFieldFocus('field-name-1', true);

        expect(fieldsRegistry['field-name-1'].highlight).toHaveBeenCalledTimes(1);
        expect(fieldsRegistry['field-name-2'].unhighlight).toHaveBeenCalledTimes(1);
        expect(fieldsRegistry['field-name-2'].unhighlight).toHaveBeenCalledWith(true);
    });

    it('при последующем блуре вызывает у всех полей highlight', () => {
        const props: UseCreditFormBlockFocusParams<FormikValues> = {
            fieldsRegistry,
            highlightFocusedField: true,
            childrenRef,
            onFieldFocusChange,
            validateField,
            values,
            errors,
            touched,
        };

        const {
            result,
        } = renderHook((props) => useCreditFormBlockFocus(props), {
            initialProps: props,
        });

        result.current.handleFieldFocus('field-name-1', true);
        result.current.handleFieldFocus('field-name-1', false);

        expect(fieldsRegistry['field-name-1'].highlight).toHaveBeenCalledTimes(2);
        expect(fieldsRegistry['field-name-2'].highlight).toHaveBeenCalledTimes(1);
    });

    it('не вызывает highlight и unhighlight, если не передан highlightFocusedField', () => {
        const props: UseCreditFormBlockFocusParams<FormikValues> = {
            fieldsRegistry,
            highlightFocusedField: false,
            childrenRef,
            onFieldFocusChange,
            validateField,
            values,
            errors,
            touched,
        };

        const {
            result,
        } = renderHook((props) => useCreditFormBlockFocus(props), {
            initialProps: props,
        });

        result.current.handleFieldFocus('field-name-1', true);

        expect(fieldsRegistry['field-name-1'].highlight).not.toHaveBeenCalled();
        expect(fieldsRegistry['field-name-1'].unhighlight).not.toHaveBeenCalled();
        expect(fieldsRegistry['field-name-2'].highlight).not.toHaveBeenCalled();
        expect(fieldsRegistry['field-name-2'].unhighlight).not.toHaveBeenCalled();
    });

    it('вызывает onFieldFocusChange, если он передан', () => {
        const props: UseCreditFormBlockFocusParams<FormikValues> = {
            fieldsRegistry,
            highlightFocusedField: false,
            childrenRef,
            onFieldFocusChange,
            validateField,
            values,
            errors,
            touched,
        };

        const {
            result,
        } = renderHook((props) => useCreditFormBlockFocus(props), {
            initialProps: props,
        });

        result.current.handleFieldFocus('field-name-1', true);

        expect(onFieldFocusChange).toHaveBeenCalledTimes(1);
        expect(onFieldFocusChange).toHaveBeenCalledWith('field-name-1', true);
    });

    it('при блуре вызывает validateField у зависимых полей', () => {
        const props: UseCreditFormBlockFocusParams<FormikValues> = {
            fieldsRegistry,
            highlightFocusedField: false,
            childrenRef,
            onFieldFocusChange,
            validateField,
            values,
            errors,
            touched,
        };

        const {
            result,
        } = renderHook((props) => useCreditFormBlockFocus(props), {
            initialProps: props,
        });

        result.current.handleFieldFocus('field-name-1', false);

        expect(validateField).toHaveBeenCalledTimes(2);
        expect(validateField).toHaveBeenNthCalledWith(1, 'field-name-2');
        expect(validateField).toHaveBeenNthCalledWith(2, 'field-name-3');
    });

    it('при фокусе НЕ вызывает validateField у зависимых полей', () => {
        const props: UseCreditFormBlockFocusParams<FormikValues> = {
            fieldsRegistry,
            highlightFocusedField: false,
            childrenRef,
            onFieldFocusChange,
            validateField,
            values,
            errors,
            touched,
        };

        const {
            result,
        } = renderHook((props) => useCreditFormBlockFocus(props), {
            initialProps: props,
        });

        result.current.handleFieldFocus('field-name-1', true);

        expect(validateField).not.toHaveBeenCalled();
    });
});

describe('focusNextField', () => {
    type TestComponentProps = {
        fieldsRegistry: CreditFormFieldsRegistry;
        childrenRef: React.RefObject<HTMLFormElement>;
    };

    const TestComponent = ({ childrenRef, fieldsRegistry }: TestComponentProps) => {
        const children = Object.keys(fieldsRegistry).map((fieldName) => {
            return (
                <div
                    key={ fieldName }
                    className="CreditFormField"
                    data-name={ fieldName }
                />
            );
        });

        return (
            <form ref={ childrenRef }>
                { children }
            </form>
        );
    };

    it('вызывает focus у следующего за текущим полем с фокусом, у которого есть ошибка, которое не потрогано или пустое', () => {
        const childrenRef: React.RefObject<HTMLFormElement> = React.createRef();
        const fieldsRegistry: CreditFormFieldsRegistry = [
            'field',
            'focused-field',
            'field-touched-with-value',
            'field-with-error',
            'field-touched-with-value-2',
            'field-untouched-with-value',
            'field-touched-with-value-3',
            'field-touched-without-value',
        ].reduce((ret, fieldName) => {
            ret[fieldName] = creditFormFieldsRegistryFieldMockGenerator();

            return ret;
        }, {} as CreditFormFieldsRegistry);

        const touched = {
            'field-touched-with-value': true,
            'field-touched-with-value-2': true,
            'field-touched-with-value-3': true,
            'field-touched-without-value': true,
        };

        const errors = {
            'field-with-error': 'error',
        };

        const values = {
            'field-with-value': 'value',
            'field-touched-with-value': 'value',
            'field-untouched-with-value': 'value',
            'field-touched-with-value-2': 'value',
            'field-touched-with-value-3': 'value',
        };

        mount(
            <TestComponent
                childrenRef={ childrenRef }
                fieldsRegistry={ fieldsRegistry }
            />,
        );

        const props: UseCreditFormBlockFocusParams<FormikValues> = {
            fieldsRegistry,
            highlightFocusedField: false,
            childrenRef,
            onFieldFocusChange,
            validateField,
            values,
            errors,
            touched,
        };

        let focusNextFieldResult: boolean;

        const {
            result,
        } = renderHook((props) => useCreditFormBlockFocus(props), {
            initialProps: props,
        });

        act(() => {
            result.current.handleFieldFocus('focused-field', true);
            focusNextFieldResult = result.current.focusNextField();
            expect(focusNextFieldResult).toEqual(true);
        });

        expect(fieldsRegistry['field-with-error'].focus).toHaveBeenCalledTimes(1);

        act(() => {
            result.current.handleFieldFocus('field-with-error', true);
            focusNextFieldResult = result.current.focusNextField();
            expect(focusNextFieldResult).toEqual(true);
        });

        expect(fieldsRegistry['field-untouched-with-value'].focus).toHaveBeenCalledTimes(1);

        act(() => {
            result.current.handleFieldFocus('field-untouched-with-value', true);
            focusNextFieldResult = result.current.focusNextField();
            expect(focusNextFieldResult).toEqual(true);
        });

        expect(fieldsRegistry['field-touched-without-value'].focus).toHaveBeenCalledTimes(1);

        act(() => {
            result.current.handleFieldFocus('field-touched-without-value', true);
            focusNextFieldResult = result.current.focusNextField();
            expect(focusNextFieldResult).toEqual(false);
        });
    });
});
