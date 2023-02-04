import { renderHook } from '@testing-library/react-hooks';
import type {
    FormikValues,
} from 'formik';

import type { CreditFormFieldsRegistry } from 'auto-core/react/components/common/CreditForm/types';

import { creditFormFieldsRegistryFieldMockGenerator } from './creditFormFieldsRegistryField.mock';
import mockRunValidationSchema from './_runValidationSchema.mock';
jest.mock('./_runValidationSchema', () => mockRunValidationSchema);
import useCreditFormBlockValues from './useCreditFormBlockValues';
import type { UseCreditFormBlockValuesParams } from './useCreditFormBlockValues';

const validateForm: jest.MockedFunction<UseCreditFormBlockValuesParams<FormikValues>['validateForm']> = jest.fn(() => Promise.resolve({}));
const setFieldValue: jest.MockedFunction<UseCreditFormBlockValuesParams<FormikValues>['setFieldValue']> = jest.fn();
let fieldsRegistry: CreditFormFieldsRegistry;
let values: UseCreditFormBlockValuesParams<FormikValues>['values'];
let errors: UseCreditFormBlockValuesParams<FormikValues>['errors'];

beforeEach(() => {
    values = {};
    errors = {};
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
});

it('validate возвращает ошибки существующих полей и ставит фокус на первом поле с ошибкой', async() => {
    const props: UseCreditFormBlockValuesParams<FormikValues> = {
        validateForm,
        setFieldValue,
        fieldsRegistry,
        values,
        errors,
    };

    const {
        result,
    } = renderHook((props) => useCreditFormBlockValues(props), {
        initialProps: props,
    });

    validateForm.mockImplementationOnce(() => Promise.resolve({
        'field-25': 'error',
        'field-name-1': undefined,
        'field-name-2': 'some error',
    }));

    const errorsResult = await result.current.validate();

    expect(validateForm).toHaveBeenCalledTimes(1);
    expect(errorsResult).toEqual({
        'field-25': 'error',
        'field-name-2': 'some error',
    });
    expect(fieldsRegistry['field-name-2'].focus).toHaveBeenCalledTimes(1);
});

it('validate возвращает undefined при отсутствии ошибок', async() => {
    const props: UseCreditFormBlockValuesParams<FormikValues> = {
        validateForm,
        setFieldValue,
        fieldsRegistry,
        values,
        errors,
    };

    const {
        result,
    } = renderHook((props) => useCreditFormBlockValues(props), {
        initialProps: props,
    });

    validateForm.mockImplementationOnce(() => Promise.resolve({
        'field-name-1': undefined,
    }));

    const errorsResult = await result.current.validate();

    expect(validateForm).toHaveBeenCalledTimes(1);
    expect(errorsResult).toBeUndefined();
});

it('validateSilently возвращает не undefined ошибки', async() => {
    const props: UseCreditFormBlockValuesParams<FormikValues> = {
        validateForm,
        setFieldValue,
        fieldsRegistry,
        values,
        errors,
        validationSchema: {} as UseCreditFormBlockValuesParams<FormikValues>['validationSchema'],
    };

    const {
        result,
    } = renderHook((props) => useCreditFormBlockValues(props), {
        initialProps: props,
    });

    mockRunValidationSchema.mockImplementationOnce(() => Promise.resolve({
        'field-25': 'error',
        'field-name-1': undefined,
        'field-name-2': 'some error',
    }));

    const errorsResult = await result.current.validateSilently();

    expect(mockRunValidationSchema).toHaveBeenCalledTimes(1);
    expect(errorsResult).toEqual({
        'field-25': 'error',
        'field-name-2': 'some error',
    });
});

it('validateSilently возвращает undefined при отсутствии ошибок', async() => {
    const props: UseCreditFormBlockValuesParams<FormikValues> = {
        validateForm,
        setFieldValue,
        fieldsRegistry,
        values,
        errors,
        validationSchema: {} as UseCreditFormBlockValuesParams<FormikValues>['validationSchema'],
    };

    const {
        result,
    } = renderHook((props) => useCreditFormBlockValues(props), {
        initialProps: props,
    });

    mockRunValidationSchema.mockImplementationOnce(() => Promise.resolve({
        'field-name-1': undefined,
    }));

    const errorsResult = await result.current.validateSilently();

    expect(mockRunValidationSchema).toHaveBeenCalledTimes(1);
    expect(errorsResult).toBeUndefined();
});

it('getValues возвращает значения полей, присутствующих в блоке', () => {
    const props: UseCreditFormBlockValuesParams<FormikValues> = {
        validateForm,
        setFieldValue,
        fieldsRegistry,
        values: {
            'field-name-2': 'value',
            'some-name': 'some',
        },
        errors,
    };

    const {
        result,
    } = renderHook((props) => useCreditFormBlockValues(props), {
        initialProps: props,
    });

    expect(result.current.getValues()).toEqual({
        'field-name-2': 'value',
    });
});

it('getValue возвращает значения поля, присутствующего в блоке', () => {
    const props: UseCreditFormBlockValuesParams<FormikValues> = {
        validateForm,
        setFieldValue,
        fieldsRegistry,
        values: {
            'field-name-2': 'value',
            'some-name': 'some',
        },
        errors,
    };

    const {
        result,
    } = renderHook((props) => useCreditFormBlockValues(props), {
        initialProps: props,
    });

    expect(result.current.getValue('field-name-2')).toEqual('value');
    expect(result.current.getValue('some-name')).toBeUndefined();
});

it('getErrors возвращает ошибки полей, присутствующих в блоке', () => {
    const props: UseCreditFormBlockValuesParams<FormikValues> = {
        validateForm,
        setFieldValue,
        fieldsRegistry,
        values,
        errors: {
            'field-name-2': 'error',
            'some-name': 'some',
        },
    };

    const {
        result,
    } = renderHook((props) => useCreditFormBlockValues(props), {
        initialProps: props,
    });

    expect(result.current.getErrors()).toEqual({
        'field-name-2': 'error',
    });
});

it('getError возвращает ошибку поля, присутствующего в блоке', () => {
    const props: UseCreditFormBlockValuesParams<FormikValues> = {
        validateForm,
        setFieldValue,
        fieldsRegistry,
        values,
        errors: {
            'field-name-2': 'error',
            'some-name': 'some',
        },
    };

    const {
        result,
    } = renderHook((props) => useCreditFormBlockValues(props), {
        initialProps: props,
    });

    expect(result.current.getError('field-name-2')).toEqual('error');
    expect(result.current.getError('some-name')).toBeUndefined();
});

it('setValue вызывает setFieldValue', () => {
    const props: UseCreditFormBlockValuesParams<FormikValues> = {
        validateForm,
        setFieldValue,
        fieldsRegistry,
        values,
        errors,
    };

    const {
        result,
    } = renderHook((props) => useCreditFormBlockValues(props), {
        initialProps: props,
    });

    result.current.setValue('name', 'value');

    expect(setFieldValue).toHaveBeenCalledTimes(1);
    expect(setFieldValue).toHaveBeenCalledWith('name', 'value');
});

/*
setValue,
*/
