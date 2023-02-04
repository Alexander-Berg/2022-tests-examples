import { renderHook, act } from '@testing-library/react-hooks';

import sleep from 'auto-core/lib/sleep';

import formFieldRegistryFieldMockChain from 'auto-core/react/components/common/Form/hooks/formFieldRegistryField.mockchain';
import formFieldRegistryMockChain from 'auto-core/react/components/common/Form/hooks/formFieldRegistry.mockchain';

import type { FormValidationResult, FormFieldRegistryField, FormErrors } from '../types';

import useFormHandlers from './useFormHandlers';
import type { UseFormHandlersProps } from './useFormHandlers';

enum ErrorTypes {
    REQUIRED = 'required',
    LOL = 'lol',
}

const Errors: Record<ErrorTypes, FormValidationResult<ErrorTypes>> = {
    [ ErrorTypes.REQUIRED ]: {
        type: ErrorTypes.REQUIRED,
        text: 'error required',
    },
    [ ErrorTypes.LOL ]: {
        type: ErrorTypes.LOL,
        text: 'error lol',
    },
};

enum FieldNames {
    FIELD_WITH_GROUP = 'group.field',
    FIELD_WITH_GROUP_2 = 'group.field2',
    FIELD = 'field',
    DEPENDENT_FIELD = 'dependent_field',
}

type Fields = {
    [ FieldNames.FIELD_WITH_GROUP ]: string;
    [ FieldNames.FIELD_WITH_GROUP_2 ]: string;
    [ FieldNames.FIELD ]: number;
    [ FieldNames.DEPENDENT_FIELD ]: number;
}

const handlersMock = {
    blurField: jest.fn(),
    getFieldNames: jest.fn(),
    getFieldValue: jest.fn(),
    getGroupFieldNames: jest.fn(),
    onFieldChange: jest.fn(),
    onSubmit: jest.fn(),
    setFieldError: jest.fn(),
    setFieldValue: jest.fn(),
    getFieldError: jest.fn(),
    isFieldTouched: jest.fn(),
};

const REGISTRY_MOCK = formFieldRegistryMockChain<FieldNames, Fields, ErrorTypes>()
    .withFields({
        [ FieldNames.FIELD_WITH_GROUP ]: formFieldRegistryFieldMockChain<FieldNames, Fields, ErrorTypes>()
            .withName(FieldNames.FIELD_WITH_GROUP),
        [ FieldNames.FIELD_WITH_GROUP_2 ]: formFieldRegistryFieldMockChain<FieldNames, Fields, ErrorTypes>()
            .withName(FieldNames.FIELD_WITH_GROUP_2)
            .withDependentFields([ FieldNames.DEPENDENT_FIELD ]),
        [ FieldNames.FIELD ]: formFieldRegistryFieldMockChain<FieldNames, Fields, ErrorTypes>()
            .withName(FieldNames.FIELD),
        [ FieldNames.DEPENDENT_FIELD ]: formFieldRegistryFieldMockChain<FieldNames, Fields, ErrorTypes>()
            .withName(FieldNames.DEPENDENT_FIELD),
    })
    .withGroups({
        'default': [ FieldNames.FIELD ],
        group: [ FieldNames.FIELD_WITH_GROUP, FieldNames.FIELD_WITH_GROUP_2 ],
    });

it('validateAllFieldsSilently валидирует поляшки и не устанавливает ошибки, возвращает их', async() => {
    const registryMock = REGISTRY_MOCK.value();

    const {
        result,
    } = renderHook((props: UseFormHandlersProps<FieldNames, Fields, ErrorTypes>) => useFormHandlers<FieldNames, Fields, ErrorTypes>(props), {
        initialProps: {
            ...handlersMock,
            state: {
                errors: {},
                touched: {},
                previousFocused: null,
                focused: null,
                values: {
                    [ FieldNames.FIELD_WITH_GROUP ]: 'lol',
                    [ FieldNames.FIELD_WITH_GROUP_2 ]: 'kek',
                    [ FieldNames.FIELD ]: 2,
                    [ FieldNames.DEPENDENT_FIELD ]: 5,
                },
            },
            fields: registryMock.fields,
        },
    });

    handlersMock.getFieldNames.mockImplementationOnce(() => {
        return Object.keys(registryMock.fields);
    });

    (
        registryMock.fields[ FieldNames.FIELD_WITH_GROUP ]?.validator as jest.MockedFunction<
        Exclude<FormFieldRegistryField<FieldNames, Fields, ErrorTypes>['validator'], undefined>
        >
    ).mockImplementationOnce(() => {
        return Promise.resolve(Errors[ ErrorTypes.REQUIRED ]);
    });
    (
        registryMock.fields[ FieldNames.FIELD_WITH_GROUP_2 ]?.validator as jest.MockedFunction<
        Exclude<FormFieldRegistryField<FieldNames, Fields, ErrorTypes>['validator'], undefined>
        >
    ).mockImplementationOnce(() => {
        return Promise.resolve({
            some: Errors[ ErrorTypes.LOL ],
            subField: Errors[ ErrorTypes.REQUIRED ],
        });
    });

    let errors;

    await act(async() => {
        errors = await result.current.validateAllFieldsSilently();

        expect(errors).toEqual({
            [ FieldNames.FIELD_WITH_GROUP ]: Errors[ ErrorTypes.REQUIRED ],
            [ FieldNames.FIELD_WITH_GROUP_2 ]: {
                some: Errors[ ErrorTypes.LOL ],
                subField: Errors[ ErrorTypes.REQUIRED ],
            },
        });
    });

    expect(handlersMock.setFieldError).not.toHaveBeenCalled();

    expect(registryMock.fields[ FieldNames.FIELD_WITH_GROUP ]?.validator).toHaveBeenCalledWith('lol', {
        [ FieldNames.FIELD_WITH_GROUP ]: 'lol',
        [ FieldNames.FIELD_WITH_GROUP_2 ]: 'kek',
        [ FieldNames.FIELD ]: 2,
        [ FieldNames.DEPENDENT_FIELD ]: 5,
    });
    expect(registryMock.fields[ FieldNames.FIELD_WITH_GROUP_2 ]?.validator).toHaveBeenCalledWith('kek', {
        [ FieldNames.FIELD_WITH_GROUP ]: 'lol',
        [ FieldNames.FIELD_WITH_GROUP_2 ]: 'kek',
        [ FieldNames.FIELD ]: 2,
        [ FieldNames.DEPENDENT_FIELD ]: 5,
    });
    expect(registryMock.fields[ FieldNames.FIELD ]?.validator).toHaveBeenCalledWith(2, {
        [ FieldNames.FIELD_WITH_GROUP ]: 'lol',
        [ FieldNames.FIELD_WITH_GROUP_2 ]: 'kek',
        [ FieldNames.FIELD ]: 2,
        [ FieldNames.DEPENDENT_FIELD ]: 5,
    });
    expect(registryMock.fields[ FieldNames.DEPENDENT_FIELD ]?.validator).toHaveBeenCalledWith(5, {
        [ FieldNames.FIELD_WITH_GROUP ]: 'lol',
        [ FieldNames.FIELD_WITH_GROUP_2 ]: 'kek',
        [ FieldNames.FIELD ]: 2,
        [ FieldNames.DEPENDENT_FIELD ]: 5,
    });
});

it('validateAllFields валидирует поляшки устанавливает ошибки, возвращает их', async() => {
    const registryMock = REGISTRY_MOCK.value();

    const {
        result,
    } = renderHook((props: UseFormHandlersProps<FieldNames, Fields, ErrorTypes>) => useFormHandlers<FieldNames, Fields, ErrorTypes>(props), {
        initialProps: {
            ...handlersMock,
            state: {
                errors: {},
                touched: {},
                previousFocused: null,
                focused: null,
                values: {
                    [ FieldNames.FIELD_WITH_GROUP ]: 'lol',
                    [ FieldNames.FIELD_WITH_GROUP_2 ]: 'kek',
                    [ FieldNames.FIELD ]: 2,
                    [ FieldNames.DEPENDENT_FIELD ]: 5,
                },
            },
            fields: registryMock.fields,
        },
    });

    handlersMock.getFieldNames.mockImplementation(() => {
        return Object.keys(registryMock.fields);
    });

    (
        registryMock.fields[ FieldNames.FIELD_WITH_GROUP ]?.validator as jest.MockedFunction<
        Exclude<FormFieldRegistryField<FieldNames, Fields, ErrorTypes>['validator'], undefined>
        >
    ).mockImplementationOnce(() => {
        return Promise.resolve(Errors[ ErrorTypes.REQUIRED ]);
    });
    (
        registryMock.fields[ FieldNames.FIELD_WITH_GROUP_2 ]?.validator as jest.MockedFunction<
        Exclude<FormFieldRegistryField<FieldNames, Fields, ErrorTypes>['validator'], undefined>
        >
    ).mockImplementationOnce(() => {
        return Promise.resolve({
            some: Errors[ ErrorTypes.LOL ],
            subField: Errors[ ErrorTypes.REQUIRED ],
        });
    });

    let errors: FormErrors<FieldNames, Fields, ErrorTypes>;

    await act(async() => {
        errors = await result.current.validateAllFields();

        expect(errors).toEqual({
            [ FieldNames.FIELD_WITH_GROUP ]: Errors[ ErrorTypes.REQUIRED ],
            [ FieldNames.FIELD_WITH_GROUP_2 ]: {
                some: Errors[ ErrorTypes.LOL ],
                subField: Errors[ ErrorTypes.REQUIRED ],
            },
        });
    });

    expect(handlersMock.setFieldError).toHaveBeenNthCalledWith(
        1, FieldNames.FIELD_WITH_GROUP, Errors[ ErrorTypes.REQUIRED ],
    );
    expect(handlersMock.setFieldError).toHaveBeenNthCalledWith(
        2, FieldNames.FIELD_WITH_GROUP_2, {
            some: Errors[ ErrorTypes.LOL ],
            subField: Errors[ ErrorTypes.REQUIRED ],
        });
    expect(handlersMock.setFieldError).toHaveBeenNthCalledWith(3, FieldNames.FIELD, undefined);
    expect(handlersMock.setFieldError).toHaveBeenNthCalledWith(4, FieldNames.DEPENDENT_FIELD, undefined);
    expect(handlersMock.setFieldError).toHaveBeenCalledTimes(4);

    expect(registryMock.fields[ FieldNames.FIELD_WITH_GROUP ]?.validator).toHaveBeenCalledWith('lol', {
        [ FieldNames.FIELD_WITH_GROUP ]: 'lol',
        [ FieldNames.FIELD_WITH_GROUP_2 ]: 'kek',
        [ FieldNames.FIELD ]: 2,
        [ FieldNames.DEPENDENT_FIELD ]: 5,
    });
    expect(registryMock.fields[ FieldNames.FIELD_WITH_GROUP_2 ]?.validator).toHaveBeenCalledWith('kek', {
        [ FieldNames.FIELD_WITH_GROUP ]: 'lol',
        [ FieldNames.FIELD_WITH_GROUP_2 ]: 'kek',
        [ FieldNames.FIELD ]: 2,
        [ FieldNames.DEPENDENT_FIELD ]: 5,
    });
    expect(registryMock.fields[ FieldNames.FIELD ]?.validator).toHaveBeenCalledWith(2, {
        [ FieldNames.FIELD_WITH_GROUP ]: 'lol',
        [ FieldNames.FIELD_WITH_GROUP_2 ]: 'kek',
        [ FieldNames.FIELD ]: 2,
        [ FieldNames.DEPENDENT_FIELD ]: 5,
    });
    expect(registryMock.fields[ FieldNames.DEPENDENT_FIELD ]?.validator).toHaveBeenCalledWith(5, {
        [ FieldNames.FIELD_WITH_GROUP ]: 'lol',
        [ FieldNames.FIELD_WITH_GROUP_2 ]: 'kek',
        [ FieldNames.FIELD ]: 2,
        [ FieldNames.DEPENDENT_FIELD ]: 5,
    });
});

it('validateGroupSilently валидирует поляшки группы с зависимыми полями и не устанавливает ошибки, возвращает их', async() => {
    const registryMock = REGISTRY_MOCK.value();

    const {
        result,
    } = renderHook((props: UseFormHandlersProps<FieldNames, Fields, ErrorTypes>) => useFormHandlers<FieldNames, Fields, ErrorTypes>(props), {
        initialProps: {
            ...handlersMock,
            state: {
                errors: {},
                touched: {},
                previousFocused: null,
                focused: null,
                values: {
                    [ FieldNames.FIELD_WITH_GROUP ]: 'lol',
                    [ FieldNames.FIELD_WITH_GROUP_2 ]: 'kek',
                    [ FieldNames.FIELD ]: 2,
                    [ FieldNames.DEPENDENT_FIELD ]: 5,
                },
            },
            fields: registryMock.fields,
        },
    });

    handlersMock.getFieldNames.mockImplementation(() => {
        return Object.keys(registryMock.fields);
    });

    handlersMock.getGroupFieldNames.mockImplementation(() => {
        return [ FieldNames.FIELD_WITH_GROUP, FieldNames.FIELD_WITH_GROUP_2 ];
    });

    (
        registryMock.fields[ FieldNames.FIELD_WITH_GROUP ]?.validator as jest.MockedFunction<
        Exclude<FormFieldRegistryField<FieldNames, Fields, ErrorTypes>['validator'], undefined>
        >
    ).mockImplementationOnce(() => {
        return Promise.resolve(Errors[ ErrorTypes.REQUIRED ]);
    });
    (
        registryMock.fields[ FieldNames.FIELD_WITH_GROUP_2 ]?.validator as jest.MockedFunction<
        Exclude<FormFieldRegistryField<FieldNames, Fields, ErrorTypes>['validator'], undefined>
        >
    ).mockImplementationOnce(() => {
        return Promise.resolve({
            some: Errors[ ErrorTypes.LOL ],
            subField: Errors[ ErrorTypes.REQUIRED ],
        });
    });

    let errors;

    await act(async() => {
        errors = await result.current.validateGroupSilently('group');
    });

    expect(errors).toEqual({
        [ FieldNames.FIELD_WITH_GROUP ]: Errors[ ErrorTypes.REQUIRED ],
        [ FieldNames.FIELD_WITH_GROUP_2 ]: {
            some: Errors[ ErrorTypes.LOL ],
            subField: Errors[ ErrorTypes.REQUIRED ],
        },
    });

    expect(handlersMock.setFieldError).not.toHaveBeenCalled();

    expect(registryMock.fields[ FieldNames.FIELD_WITH_GROUP ]?.validator).toHaveBeenCalledWith('lol', {
        [ FieldNames.FIELD_WITH_GROUP ]: 'lol',
        [ FieldNames.FIELD_WITH_GROUP_2 ]: 'kek',
        [ FieldNames.FIELD ]: 2,
        [ FieldNames.DEPENDENT_FIELD ]: 5,
    });
    expect(registryMock.fields[ FieldNames.FIELD_WITH_GROUP_2 ]?.validator).toHaveBeenCalledWith('kek', {
        [ FieldNames.FIELD_WITH_GROUP ]: 'lol',
        [ FieldNames.FIELD_WITH_GROUP_2 ]: 'kek',
        [ FieldNames.FIELD ]: 2,
        [ FieldNames.DEPENDENT_FIELD ]: 5,
    });
    expect(registryMock.fields[ FieldNames.FIELD ]?.validator).not.toHaveBeenCalled();
    expect(registryMock.fields[ FieldNames.DEPENDENT_FIELD ]?.validator).toHaveBeenCalledWith(5, {
        [ FieldNames.FIELD_WITH_GROUP ]: 'lol',
        [ FieldNames.FIELD_WITH_GROUP_2 ]: 'kek',
        [ FieldNames.FIELD ]: 2,
        [ FieldNames.DEPENDENT_FIELD ]: 5,
    });
});

it('validateGroupSilentlyWoDependencies валидирует поляшки группы без зависимых полей и не устанавливает ошибки, возвращает их', async() => {
    const registryMock = REGISTRY_MOCK.value();

    const {
        result,
    } = renderHook((props: UseFormHandlersProps<FieldNames, Fields, ErrorTypes>) => useFormHandlers<FieldNames, Fields, ErrorTypes>(props), {
        initialProps: {
            ...handlersMock,
            state: {
                errors: {},
                touched: {},
                previousFocused: null,
                focused: null,
                values: {
                    [ FieldNames.FIELD_WITH_GROUP ]: 'lol',
                    [ FieldNames.FIELD_WITH_GROUP_2 ]: 'kek',
                    [ FieldNames.FIELD ]: 2,
                    [ FieldNames.DEPENDENT_FIELD ]: 5,
                },
            },
            fields: registryMock.fields,
        },
    });

    handlersMock.getFieldNames.mockImplementation(() => {
        return Object.keys(registryMock.fields);
    });

    handlersMock.getGroupFieldNames.mockImplementation(() => {
        return [ FieldNames.FIELD_WITH_GROUP, FieldNames.FIELD_WITH_GROUP_2 ];
    });

    (
        registryMock.fields[ FieldNames.FIELD_WITH_GROUP ]?.validator as jest.MockedFunction<
        Exclude<FormFieldRegistryField<FieldNames, Fields, ErrorTypes>['validator'], undefined>
        >
    ).mockImplementationOnce(() => {
        return Promise.resolve(Errors[ ErrorTypes.REQUIRED ]);
    });
    (
        registryMock.fields[ FieldNames.FIELD_WITH_GROUP_2 ]?.validator as jest.MockedFunction<
        Exclude<FormFieldRegistryField<FieldNames, Fields, ErrorTypes>['validator'], undefined>
        >
    ).mockImplementationOnce(() => {
        return Promise.resolve({
            some: Errors[ ErrorTypes.LOL ],
            subField: Errors[ ErrorTypes.REQUIRED ],
        });
    });

    let errors;

    await act(async() => {
        errors = await result.current.validateGroupSilentlyWoDependencies('group');
    });

    expect(errors).toEqual({
        [ FieldNames.FIELD_WITH_GROUP ]: Errors[ ErrorTypes.REQUIRED ],
        [ FieldNames.FIELD_WITH_GROUP_2 ]: {
            some: Errors[ ErrorTypes.LOL ],
            subField: Errors[ ErrorTypes.REQUIRED ],
        },
    });

    expect(handlersMock.setFieldError).not.toHaveBeenCalled();

    expect(registryMock.fields[ FieldNames.FIELD_WITH_GROUP ]?.validator).toHaveBeenCalledWith('lol', {
        [ FieldNames.FIELD_WITH_GROUP ]: 'lol',
        [ FieldNames.FIELD_WITH_GROUP_2 ]: 'kek',
        [ FieldNames.FIELD ]: 2,
        [ FieldNames.DEPENDENT_FIELD ]: 5,
    });
    expect(registryMock.fields[ FieldNames.FIELD_WITH_GROUP_2 ]?.validator).toHaveBeenCalledWith('kek', {
        [ FieldNames.FIELD_WITH_GROUP ]: 'lol',
        [ FieldNames.FIELD_WITH_GROUP_2 ]: 'kek',
        [ FieldNames.FIELD ]: 2,
        [ FieldNames.DEPENDENT_FIELD ]: 5,
    });
    expect(registryMock.fields[ FieldNames.FIELD ]?.validator).not.toHaveBeenCalled();
    expect(registryMock.fields[ FieldNames.DEPENDENT_FIELD ]?.validator).not.toHaveBeenCalled();
});

it('validateGroup валидирует поляшки группы с зависимостями^ устанавливает ошибки, возвращает их', async() => {
    const registryMock = REGISTRY_MOCK.value();

    const {
        result,
    } = renderHook((props: UseFormHandlersProps<FieldNames, Fields, ErrorTypes>) => useFormHandlers<FieldNames, Fields, ErrorTypes>(props), {
        initialProps: {
            ...handlersMock,
            state: {
                errors: {},
                touched: {},
                previousFocused: null,
                focused: null,
                values: {
                    [ FieldNames.FIELD_WITH_GROUP ]: 'lol',
                    [ FieldNames.FIELD_WITH_GROUP_2 ]: 'kek',
                    [ FieldNames.FIELD ]: 2,
                    [ FieldNames.DEPENDENT_FIELD ]: 5,
                },
            },
            fields: registryMock.fields,
        },
    });

    handlersMock.getFieldNames.mockImplementation(() => {
        return Object.keys(registryMock.fields);
    });

    handlersMock.getGroupFieldNames.mockImplementation(() => {
        return [ FieldNames.FIELD_WITH_GROUP, FieldNames.FIELD_WITH_GROUP_2 ];
    });

    (
        registryMock.fields[ FieldNames.FIELD_WITH_GROUP ]?.validator as jest.MockedFunction<
        Exclude<FormFieldRegistryField<FieldNames, Fields, ErrorTypes>['validator'], undefined>
        >
    ).mockImplementationOnce(() => {
        return Promise.resolve(Errors[ ErrorTypes.REQUIRED ]);
    });
    (
        registryMock.fields[ FieldNames.FIELD_WITH_GROUP_2 ]?.validator as jest.MockedFunction<
        Exclude<FormFieldRegistryField<FieldNames, Fields, ErrorTypes>['validator'], undefined>
        >
    ).mockImplementationOnce(() => {
        return Promise.resolve({
            some: Errors[ ErrorTypes.LOL ],
            subField: Errors[ ErrorTypes.REQUIRED ],
        });
    });

    let errors;

    await act(async() => {
        errors = await result.current.validateGroup('group');
    });

    expect(errors).toEqual({
        [ FieldNames.FIELD_WITH_GROUP ]: Errors[ ErrorTypes.REQUIRED ],
        [ FieldNames.FIELD_WITH_GROUP_2 ]: {
            some: Errors[ ErrorTypes.LOL ],
            subField: Errors[ ErrorTypes.REQUIRED ],
        },
    });

    expect(handlersMock.setFieldError).toHaveBeenNthCalledWith(
        1, FieldNames.FIELD_WITH_GROUP, Errors[ ErrorTypes.REQUIRED ],
    );
    expect(handlersMock.setFieldError).toHaveBeenNthCalledWith(
        2, FieldNames.FIELD_WITH_GROUP_2, {
            some: Errors[ ErrorTypes.LOL ],
            subField: Errors[ ErrorTypes.REQUIRED ],
        });
    expect(handlersMock.setFieldError).toHaveBeenNthCalledWith(3, FieldNames.DEPENDENT_FIELD, undefined);
    expect(handlersMock.setFieldError).toHaveBeenCalledTimes(3);

    expect(registryMock.fields[ FieldNames.FIELD_WITH_GROUP ]?.validator).toHaveBeenCalledWith('lol', {
        [ FieldNames.FIELD_WITH_GROUP ]: 'lol',
        [ FieldNames.FIELD_WITH_GROUP_2 ]: 'kek',
        [ FieldNames.FIELD ]: 2,
        [ FieldNames.DEPENDENT_FIELD ]: 5,
    });
    expect(registryMock.fields[ FieldNames.FIELD_WITH_GROUP_2 ]?.validator).toHaveBeenCalledWith('kek', {
        [ FieldNames.FIELD_WITH_GROUP ]: 'lol',
        [ FieldNames.FIELD_WITH_GROUP_2 ]: 'kek',
        [ FieldNames.FIELD ]: 2,
        [ FieldNames.DEPENDENT_FIELD ]: 5,
    });
    expect(registryMock.fields[ FieldNames.FIELD ]?.validator).not.toHaveBeenCalled();
    expect(registryMock.fields[ FieldNames.DEPENDENT_FIELD ]?.validator).toHaveBeenCalledWith(5, {
        [ FieldNames.FIELD_WITH_GROUP ]: 'lol',
        [ FieldNames.FIELD_WITH_GROUP_2 ]: 'kek',
        [ FieldNames.FIELD ]: 2,
        [ FieldNames.DEPENDENT_FIELD ]: 5,
    });
});
it('validateGroupWoDependencies валидирует поляшки группы без зависимостей, устанавливает ошибки, возвращает их', async() => {
    const registryMock = REGISTRY_MOCK.value();

    const {
        result,
    } = renderHook((props: UseFormHandlersProps<FieldNames, Fields, ErrorTypes>) => useFormHandlers<FieldNames, Fields, ErrorTypes>(props), {
        initialProps: {
            ...handlersMock,
            state: {
                errors: {},
                touched: {},
                previousFocused: null,
                focused: null,
                values: {
                    [ FieldNames.FIELD_WITH_GROUP ]: 'lol',
                    [ FieldNames.FIELD_WITH_GROUP_2 ]: 'kek',
                    [ FieldNames.FIELD ]: 2,
                    [ FieldNames.DEPENDENT_FIELD ]: 5,
                },
            },
            fields: registryMock.fields,
        },
    });

    handlersMock.getFieldNames.mockImplementation(() => {
        return Object.keys(registryMock.fields);
    });

    handlersMock.getGroupFieldNames.mockImplementation(() => {
        return [ FieldNames.FIELD_WITH_GROUP, FieldNames.FIELD_WITH_GROUP_2 ];
    });

    (
        registryMock.fields[ FieldNames.FIELD_WITH_GROUP ]?.validator as jest.MockedFunction<
        Exclude<FormFieldRegistryField<FieldNames, Fields, ErrorTypes>['validator'], undefined>
        >
    ).mockImplementationOnce(() => {
        return Promise.resolve(Errors[ ErrorTypes.REQUIRED ]);
    });
    (
        registryMock.fields[ FieldNames.FIELD_WITH_GROUP_2 ]?.validator as jest.MockedFunction<
        Exclude<FormFieldRegistryField<FieldNames, Fields, ErrorTypes>['validator'], undefined>
        >
    ).mockImplementationOnce(() => {
        return Promise.resolve({
            some: Errors[ ErrorTypes.LOL ],
            subField: Errors[ ErrorTypes.REQUIRED ],
        });
    });

    let errors;

    await act(async() => {
        errors = await result.current.validateGroupWoDependencies('group');
    });

    expect(errors).toEqual({
        [ FieldNames.FIELD_WITH_GROUP ]: Errors[ ErrorTypes.REQUIRED ],
        [ FieldNames.FIELD_WITH_GROUP_2 ]: {
            some: Errors[ ErrorTypes.LOL ],
            subField: Errors[ ErrorTypes.REQUIRED ],
        },
    });

    expect(handlersMock.setFieldError).toHaveBeenNthCalledWith(
        1, FieldNames.FIELD_WITH_GROUP, Errors[ ErrorTypes.REQUIRED ],
    );
    expect(handlersMock.setFieldError).toHaveBeenNthCalledWith(
        2, FieldNames.FIELD_WITH_GROUP_2, {
            some: Errors[ ErrorTypes.LOL ],
            subField: Errors[ ErrorTypes.REQUIRED ],
        });
    expect(handlersMock.setFieldError).toHaveBeenNthCalledWith(3, FieldNames.DEPENDENT_FIELD, undefined);
    expect(handlersMock.setFieldError).toHaveBeenCalledTimes(3);

    expect(registryMock.fields[ FieldNames.FIELD_WITH_GROUP ]?.validator).toHaveBeenCalledWith('lol', {
        [ FieldNames.FIELD_WITH_GROUP ]: 'lol',
        [ FieldNames.FIELD_WITH_GROUP_2 ]: 'kek',
        [ FieldNames.FIELD ]: 2,
        [ FieldNames.DEPENDENT_FIELD ]: 5,
    });
    expect(registryMock.fields[ FieldNames.FIELD_WITH_GROUP_2 ]?.validator).toHaveBeenCalledWith('kek', {
        [ FieldNames.FIELD_WITH_GROUP ]: 'lol',
        [ FieldNames.FIELD_WITH_GROUP_2 ]: 'kek',
        [ FieldNames.FIELD ]: 2,
        [ FieldNames.DEPENDENT_FIELD ]: 5,
    });
    expect(registryMock.fields[ FieldNames.FIELD ]?.validator).not.toHaveBeenCalled();
    expect(registryMock.fields[ FieldNames.DEPENDENT_FIELD ]?.validator).not.toHaveBeenCalled();
});

it('validateFieldSilently валидирует поле с зависимыми полями и не устанавливает ошибки, возвращает их', async() => {
    const registryMock = REGISTRY_MOCK.value();

    const {
        result,
    } = renderHook((props: UseFormHandlersProps<FieldNames, Fields, ErrorTypes>) => useFormHandlers<FieldNames, Fields, ErrorTypes>(props), {
        initialProps: {
            ...handlersMock,
            state: {
                errors: {},
                touched: {},
                previousFocused: null,
                focused: null,
                values: {
                    [ FieldNames.FIELD_WITH_GROUP ]: 'lol',
                    [ FieldNames.FIELD_WITH_GROUP_2 ]: 'kek',
                    [ FieldNames.FIELD ]: 2,
                    [ FieldNames.DEPENDENT_FIELD ]: 5,
                },
            },
            fields: registryMock.fields,
        },
    });

    handlersMock.getFieldNames.mockImplementation(() => {
        return Object.keys(registryMock.fields);
    });

    (
        registryMock.fields[ FieldNames.FIELD_WITH_GROUP_2 ]?.validator as jest.MockedFunction<
        Exclude<FormFieldRegistryField<FieldNames, Fields, ErrorTypes>['validator'], undefined>
        >
    ).mockImplementationOnce(() => {
        return Promise.resolve({
            some: Errors[ ErrorTypes.LOL ],
            subField: Errors[ ErrorTypes.REQUIRED ],
        });
    });

    let errors;

    await act(async() => {
        errors = await result.current.validateFieldSilently(FieldNames.FIELD_WITH_GROUP_2);
    });

    expect(errors).toEqual({
        some: Errors[ ErrorTypes.LOL ],
        subField: Errors[ ErrorTypes.REQUIRED ],
    });

    expect(handlersMock.setFieldError).not.toHaveBeenCalled();

    expect(registryMock.fields[ FieldNames.FIELD_WITH_GROUP ]?.validator).not.toHaveBeenCalled();
    expect(registryMock.fields[ FieldNames.FIELD_WITH_GROUP_2 ]?.validator).toHaveBeenCalledWith('kek', {
        [ FieldNames.FIELD_WITH_GROUP ]: 'lol',
        [ FieldNames.FIELD_WITH_GROUP_2 ]: 'kek',
        [ FieldNames.FIELD ]: 2,
        [ FieldNames.DEPENDENT_FIELD ]: 5,
    });
    expect(registryMock.fields[ FieldNames.FIELD ]?.validator).not.toHaveBeenCalled();
    expect(registryMock.fields[ FieldNames.DEPENDENT_FIELD ]?.validator).toHaveBeenCalledWith(5, {
        [ FieldNames.FIELD_WITH_GROUP ]: 'lol',
        [ FieldNames.FIELD_WITH_GROUP_2 ]: 'kek',
        [ FieldNames.FIELD ]: 2,
        [ FieldNames.DEPENDENT_FIELD ]: 5,
    });
});

it('validateField валидирует поле с зависимыми полями и устанавливает ошибки, возвращает их', async() => {
    const registryMock = REGISTRY_MOCK.value();

    const {
        result,
    } = renderHook((props: UseFormHandlersProps<FieldNames, Fields, ErrorTypes>) => useFormHandlers<FieldNames, Fields, ErrorTypes>(props), {
        initialProps: {
            ...handlersMock,
            state: {
                errors: {},
                touched: {},
                previousFocused: null,
                focused: null,
                values: {
                    [ FieldNames.FIELD_WITH_GROUP ]: 'lol',
                    [ FieldNames.FIELD_WITH_GROUP_2 ]: 'kek',
                    [ FieldNames.FIELD ]: 2,
                    [ FieldNames.DEPENDENT_FIELD ]: 5,
                },
            },
            fields: registryMock.fields,
        },
    });

    handlersMock.getFieldNames.mockImplementation(() => {
        return Object.keys(registryMock.fields);
    });

    (
        registryMock.fields[ FieldNames.FIELD_WITH_GROUP_2 ]?.validator as jest.MockedFunction<
        Exclude<FormFieldRegistryField<FieldNames, Fields, ErrorTypes>['validator'], undefined>
        >
    ).mockImplementationOnce(() => {
        return Promise.resolve({
            some: Errors[ ErrorTypes.LOL ],
            subField: Errors[ ErrorTypes.REQUIRED ],
        });
    });

    let errors;

    await act(async() => {
        errors = await result.current.validateField(FieldNames.FIELD_WITH_GROUP_2);
    });

    expect(errors).toEqual({
        some: Errors[ ErrorTypes.LOL ],
        subField: Errors[ ErrorTypes.REQUIRED ],
    });

    expect(handlersMock.setFieldError).toHaveBeenNthCalledWith(
        1, FieldNames.FIELD_WITH_GROUP_2, {
            some: Errors[ ErrorTypes.LOL ],
            subField: Errors[ ErrorTypes.REQUIRED ],
        },
    );
    expect(handlersMock.setFieldError).toHaveBeenNthCalledWith(
        2, FieldNames.DEPENDENT_FIELD, undefined,
    );

    expect(registryMock.fields[ FieldNames.FIELD_WITH_GROUP ]?.validator).not.toHaveBeenCalled();
    expect(registryMock.fields[ FieldNames.FIELD_WITH_GROUP_2 ]?.validator).toHaveBeenCalledWith('kek', {
        [ FieldNames.FIELD_WITH_GROUP ]: 'lol',
        [ FieldNames.FIELD_WITH_GROUP_2 ]: 'kek',
        [ FieldNames.FIELD ]: 2,
        [ FieldNames.DEPENDENT_FIELD ]: 5,
    });
    expect(registryMock.fields[ FieldNames.FIELD ]?.validator).not.toHaveBeenCalled();
    expect(registryMock.fields[ FieldNames.DEPENDENT_FIELD ]?.validator).toHaveBeenCalledWith(5, {
        [ FieldNames.FIELD_WITH_GROUP ]: 'lol',
        [ FieldNames.FIELD_WITH_GROUP_2 ]: 'kek',
        [ FieldNames.FIELD ]: 2,
        [ FieldNames.DEPENDENT_FIELD ]: 5,
    });
});

it('submitForm валидирует форму и запускает onSubmit с ошибками', async() => {
    const registryMock = REGISTRY_MOCK.value();
    const onSubmit = jest.fn();

    const {
        result,
    } = renderHook((props: UseFormHandlersProps<FieldNames, Fields, ErrorTypes>) => useFormHandlers<FieldNames, Fields, ErrorTypes>(props), {
        initialProps: {
            ...handlersMock,
            onSubmit,
            state: {
                errors: {},
                touched: {},
                previousFocused: null,
                focused: null,
                values: {
                    [ FieldNames.FIELD_WITH_GROUP ]: 'lol',
                    [ FieldNames.FIELD_WITH_GROUP_2 ]: 'kek',
                    [ FieldNames.FIELD ]: 2,
                    [ FieldNames.DEPENDENT_FIELD ]: 5,
                },
            },
            fields: registryMock.fields,
        },
    });

    handlersMock.getFieldNames.mockImplementationOnce(() => {
        return Object.keys(registryMock.fields);
    });

    (
        registryMock.fields[ FieldNames.FIELD_WITH_GROUP_2 ]?.validator as jest.MockedFunction<
        Exclude<FormFieldRegistryField<FieldNames, Fields, ErrorTypes>['validator'], undefined>
        >
    ).mockImplementationOnce(() => {
        return Promise.resolve({
            some: Errors[ ErrorTypes.LOL ],
            subField: Errors[ ErrorTypes.REQUIRED ],
        });
    });

    await act(async() => {
        await result.current.submitForm();
    });

    expect(registryMock.fields[ FieldNames.FIELD_WITH_GROUP_2 ]?.validator).toHaveBeenCalled();

    expect(handlersMock.setFieldError).toHaveBeenNthCalledWith(
        1, FieldNames.FIELD_WITH_GROUP, undefined,
    );

    expect(handlersMock.setFieldError).toHaveBeenNthCalledWith(
        2, FieldNames.FIELD_WITH_GROUP_2, {
            some: Errors[ ErrorTypes.LOL ],
            subField: Errors[ ErrorTypes.REQUIRED ],
        },
    );

    expect(handlersMock.setFieldError).toHaveBeenNthCalledWith(
        3, FieldNames.FIELD, undefined,
    );

    expect(handlersMock.setFieldError).toHaveBeenNthCalledWith(
        4, FieldNames.DEPENDENT_FIELD, undefined,
    );

    expect(onSubmit).toHaveBeenCalledWith({
        'group.field2': {
            some: { text: 'error lol', type: 'lol' },
            subField: { text: 'error required', type: 'required' },
        },
    });
});

it('submitForm валидирует форму и запускает onSubmit, если нет ошибок', async() => {
    const registryMock = REGISTRY_MOCK.value();
    const onSubmit = jest.fn();

    const {
        result,
    } = renderHook((props: UseFormHandlersProps<FieldNames, Fields, ErrorTypes>) => useFormHandlers<FieldNames, Fields, ErrorTypes>(props), {
        initialProps: {
            ...handlersMock,
            onSubmit,
            state: {
                errors: {},
                touched: {},
                previousFocused: null,
                focused: null,
                values: {
                    [ FieldNames.FIELD_WITH_GROUP ]: 'lol',
                    [ FieldNames.FIELD_WITH_GROUP_2 ]: 'kek',
                    [ FieldNames.FIELD ]: 2,
                    [ FieldNames.DEPENDENT_FIELD ]: 5,
                },
            },
            fields: registryMock.fields,
        },
    });

    handlersMock.getFieldNames.mockImplementationOnce(() => {
        return Object.keys(registryMock.fields);
    });

    await act(async() => {
        await result.current.submitForm();
    });

    expect(onSubmit).toHaveBeenCalled();
});

it('setFieldValue не вызываает изменений при равном новом значении', async() => {
    const registryMock = REGISTRY_MOCK.value();
    const setFieldValue = jest.fn();
    const setFieldError = jest.fn();
    const getFieldValue = jest.fn();

    const {
        result,
    } = renderHook((props: UseFormHandlersProps<FieldNames, Fields, ErrorTypes>) => useFormHandlers<FieldNames, Fields, ErrorTypes>(props), {
        initialProps: {
            ...handlersMock,
            setFieldValue,
            getFieldValue,
            setFieldError,
            state: {
                errors: {},
                touched: {},
                previousFocused: null,
                focused: null,
                values: {
                    [ FieldNames.FIELD_WITH_GROUP ]: 'lol',
                    [ FieldNames.FIELD_WITH_GROUP_2 ]: 'kek',
                    [ FieldNames.FIELD ]: 2,
                    [ FieldNames.DEPENDENT_FIELD ]: 5,
                },
            },
            fields: registryMock.fields,
        },
    });

    handlersMock.getFieldNames.mockImplementationOnce(() => {
        return Object.keys(registryMock.fields);
    });

    getFieldValue.mockImplementationOnce(() => {
        return 'lol';
    });

    await act(async() => {
        result.current.setFieldValue(FieldNames.FIELD_WITH_GROUP, 'lol');

        await sleep(0);
    });

    expect(setFieldValue).not.toHaveBeenCalled();
});

it('setFieldValue вызывает изменение значения поля, скидывает ошибку и вызывает onFieldChange', async() => {
    const registryMock = REGISTRY_MOCK.value();
    const setFieldValue = jest.fn();
    const setFieldError = jest.fn();
    const getFieldValue = jest.fn();
    const onFieldChange = jest.fn();

    const {
        result,
    } = renderHook((props: UseFormHandlersProps<FieldNames, Fields, ErrorTypes>) => useFormHandlers<FieldNames, Fields, ErrorTypes>(props), {
        initialProps: {
            ...handlersMock,
            setFieldValue,
            getFieldValue,
            setFieldError,
            onFieldChange,
            state: {
                errors: {},
                touched: {},
                previousFocused: null,
                focused: null,
                values: {
                    [ FieldNames.FIELD_WITH_GROUP ]: 'lol',
                    [ FieldNames.FIELD_WITH_GROUP_2 ]: 'kek',
                    [ FieldNames.FIELD ]: 2,
                    [ FieldNames.DEPENDENT_FIELD ]: 5,
                },
            },
            fields: registryMock.fields,
        },
    });

    handlersMock.getFieldNames.mockImplementationOnce(() => {
        return Object.keys(registryMock.fields);
    });

    getFieldValue.mockImplementationOnce(() => {
        return 'kek';
    });

    await act(async() => {
        result.current.setFieldValue(FieldNames.FIELD_WITH_GROUP, 'lol');
        expect(onFieldChange).not.toHaveBeenCalled();

        await sleep(0);
    });

    expect(setFieldValue).toHaveBeenCalledWith(FieldNames.FIELD_WITH_GROUP, 'lol');
    expect(setFieldError).toHaveBeenCalledWith(FieldNames.FIELD_WITH_GROUP, undefined);
    expect(onFieldChange).toHaveBeenCalledWith(FieldNames.FIELD_WITH_GROUP, 'lol');
});

it('getGroupValues возвращает значения полей группы', async() => {
    const registryMock = REGISTRY_MOCK.value();

    const {
        result,
    } = renderHook((props: UseFormHandlersProps<FieldNames, Fields, ErrorTypes>) => useFormHandlers<FieldNames, Fields, ErrorTypes>(props), {
        initialProps: {
            ...handlersMock,
            state: {
                errors: {},
                touched: {},
                previousFocused: null,
                focused: null,
                values: {
                    [ FieldNames.FIELD_WITH_GROUP ]: 'lol',
                    [ FieldNames.FIELD_WITH_GROUP_2 ]: 'kek',
                    [ FieldNames.FIELD ]: 2,
                    [ FieldNames.DEPENDENT_FIELD ]: 5,
                },
            },
            fields: registryMock.fields,
        },
    });

    handlersMock.getGroupFieldNames.mockImplementation(() => {
        return [ FieldNames.FIELD_WITH_GROUP, FieldNames.FIELD_WITH_GROUP_2 ];
    });

    handlersMock.getFieldValue.mockImplementationOnce(() => 'lol');
    handlersMock.getFieldValue.mockImplementationOnce(() => 'kek');

    act(() => {
        const values = result.current.getGroupValues('group');

        expect(values).toEqual({
            [ FieldNames.FIELD_WITH_GROUP ]: 'lol',
            [ FieldNames.FIELD_WITH_GROUP_2 ]: 'kek',
        });
    });

    expect(handlersMock.getGroupFieldNames).toHaveBeenCalledWith('group');
});

it('getGroupErrors возвращает ошибки полей группы', async() => {
    const registryMock = REGISTRY_MOCK.value();

    const {
        result,
    } = renderHook((props: UseFormHandlersProps<FieldNames, Fields, ErrorTypes>) => useFormHandlers<FieldNames, Fields, ErrorTypes>(props), {
        initialProps: {
            ...handlersMock,
            state: {
                errors: {
                    [ FieldNames.FIELD_WITH_GROUP ]: {
                        type: ErrorTypes.REQUIRED,
                        text: 'error',
                    },
                    [ FieldNames.FIELD ]: {
                        type: ErrorTypes.LOL,
                        text: 'error2',
                    },
                },
                touched: {},
                previousFocused: null,
                focused: null,
                values: {
                    [ FieldNames.FIELD_WITH_GROUP ]: 'lol',
                    [ FieldNames.FIELD_WITH_GROUP_2 ]: 'kek',
                    [ FieldNames.FIELD ]: 2,
                    [ FieldNames.DEPENDENT_FIELD ]: 5,
                },
            },
            fields: registryMock.fields,
        },
    });

    handlersMock.getFieldError.mockImplementationOnce(() => ({
        type: ErrorTypes.REQUIRED,
        text: 'error',
    }));

    handlersMock.getGroupFieldNames.mockImplementation(() => {
        return [ FieldNames.FIELD_WITH_GROUP, FieldNames.FIELD_WITH_GROUP_2 ];
    });

    act(() => {
        const errors = result.current.getGroupErrors('group');

        expect(errors).toEqual({
            [ FieldNames.FIELD_WITH_GROUP ]: {
                type: ErrorTypes.REQUIRED,
                text: 'error',
            },
        });
    });

    expect(handlersMock.getGroupFieldNames).toHaveBeenCalledWith('group');
});

it('getGroupTouchedFields возвращает список потроганных полей в группе', async() => {
    const registryMock = REGISTRY_MOCK.value();

    const {
        result,
    } = renderHook((props: UseFormHandlersProps<FieldNames, Fields, ErrorTypes>) => useFormHandlers<FieldNames, Fields, ErrorTypes>(props), {
        initialProps: {
            ...handlersMock,
            state: {
                errors: {
                    [ FieldNames.FIELD_WITH_GROUP ]: {
                        type: ErrorTypes.REQUIRED,
                        text: 'error',
                    },
                    [ FieldNames.FIELD ]: {
                        type: ErrorTypes.LOL,
                        text: 'error2',
                    },
                },
                touched: {
                    [ FieldNames.FIELD_WITH_GROUP ]: true,
                },
                previousFocused: null,
                focused: null,
                values: {
                    [ FieldNames.FIELD_WITH_GROUP ]: 'lol',
                    [ FieldNames.FIELD_WITH_GROUP_2 ]: 'kek',
                    [ FieldNames.FIELD ]: 2,
                    [ FieldNames.DEPENDENT_FIELD ]: 5,
                },
            },
            fields: registryMock.fields,
        },
    });

    handlersMock.isFieldTouched.mockImplementationOnce(() => true);

    handlersMock.getGroupFieldNames.mockImplementation(() => {
        return [ FieldNames.FIELD_WITH_GROUP, FieldNames.FIELD_WITH_GROUP_2 ];
    });

    act(() => {
        const touched = result.current.getGroupTouchedFields('group');

        expect(touched).toEqual([ FieldNames.FIELD_WITH_GROUP ]);
    });

    expect(handlersMock.getGroupFieldNames).toHaveBeenCalledWith('group');
});
