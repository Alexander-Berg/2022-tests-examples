import { renderHook, act } from '@testing-library/react-hooks';

import useFormState from './useFormState';

enum Errors {
    REQUIRED = 'required',
}

enum FieldNames {
    FIELD_WITH_GROUP= 'group.field',
    FIELD = 'field',
}

type Fields = {
    [ FieldNames.FIELD_WITH_GROUP ]: string;
    [ FieldNames.FIELD ]: number;
}

it('initialValues устанавливает изначальные значения', () => {
    const {
        result: {
            current: {
                state,
            },
        },
    } = renderHook(() => useFormState<FieldNames, Fields, Errors>({
        initialValues: {
            [ FieldNames.FIELD ]: 2,
        },
    }));

    expect(state.values[ FieldNames.FIELD ]).toEqual(2);
});

it('getFieldValues возвращает значения всех полей', () => {
    const {
        result: {
            current: {
                getFieldValues,
            },
        },
    } = renderHook(() => useFormState<FieldNames, Fields, Errors>({
        initialValues: {
            [ FieldNames.FIELD ]: 2,
            [ FieldNames.FIELD_WITH_GROUP ]: 'lol',
        },
    }));

    expect(getFieldValues()).toEqual({
        [ FieldNames.FIELD ]: 2,
        [ FieldNames.FIELD_WITH_GROUP ]: 'lol',
    });
});

it('getFieldValue возвращает значение поля', () => {
    const {
        result: {
            current: {
                getFieldValue,
            },
        },
    } = renderHook(() => useFormState<FieldNames, Fields, Errors>({
        initialValues: {
            [ FieldNames.FIELD ]: 2,
        },
    }));

    expect(getFieldValue(FieldNames.FIELD)).toEqual(2);
    expect(getFieldValue(FieldNames.FIELD_WITH_GROUP)).toEqual(undefined);
});

it('setFieldValue устанавливает значение поля', () => {
    const {
        result: {
            current: {
                getFieldValue,
                setFieldValue,
            },
        },
    } = renderHook(() => useFormState<FieldNames, Fields, Errors>({
        initialValues: {
            [ FieldNames.FIELD ]: 2,
            [ FieldNames.FIELD_WITH_GROUP ]: 'lol',
        },
    }));

    act(() => {
        setFieldValue(FieldNames.FIELD_WITH_GROUP, 'loooool');
    });

    expect(getFieldValue(FieldNames.FIELD_WITH_GROUP)).toEqual('loooool');
});

it('setFieldValue удаляет значение поля', () => {
    const {
        result,
    } = renderHook(() => useFormState<FieldNames, Fields, Errors>({
        initialValues: {
            [ FieldNames.FIELD ]: 2,
            [ FieldNames.FIELD_WITH_GROUP ]: 'lol',
        },
    }));

    const {
        current: {
            setFieldValue,
        },
    } = result;

    act(() => {
        setFieldValue(FieldNames.FIELD_WITH_GROUP, undefined);
    });

    expect(result.current.state.values).toEqual({
        [ FieldNames.FIELD ]: 2,
    });
});

it('setFieldError устанавливает и удаляет ошибку', () => {
    const {
        result,
    } = renderHook(() => useFormState<FieldNames, Fields, Errors>({
        initialValues: {
            [ FieldNames.FIELD ]: 2,
            [ FieldNames.FIELD_WITH_GROUP ]: 'lol',
        },
    }));

    const {
        current: {
            setFieldError,
        },
    } = result;

    act(() => {
        setFieldError(FieldNames.FIELD_WITH_GROUP, {
            type: Errors.REQUIRED,
            text: 'error',
        });

        setFieldError(FieldNames.FIELD, {
            type: Errors.REQUIRED,
            text: 'error2',
        });
    });

    expect(result.current.state.errors).toEqual({
        [ FieldNames.FIELD_WITH_GROUP ]: {
            type: Errors.REQUIRED,
            text: 'error',
        },
        [ FieldNames.FIELD ]: {
            type: Errors.REQUIRED,
            text: 'error2',
        },
    });

    act(() => {
        setFieldError(FieldNames.FIELD_WITH_GROUP, undefined);
    });

    expect(result.current.state.errors).toEqual({
        [ FieldNames.FIELD ]: {
            type: Errors.REQUIRED,
            text: 'error2',
        },
    });
});

it('getFieldErrors возвращает все ошибки', () => {
    const {
        result,
    } = renderHook(() => useFormState<FieldNames, Fields, Errors>({
        initialValues: {
            [ FieldNames.FIELD ]: 2,
            [ FieldNames.FIELD_WITH_GROUP ]: 'lol',
        },
    }));

    const {
        current: {
            getFieldErrors,
            setFieldError,
        },
    } = result;

    act(() => {
        setFieldError(FieldNames.FIELD_WITH_GROUP, {
            type: Errors.REQUIRED,
            text: 'error',
        });
    });

    expect(getFieldErrors()).toEqual({
        [ FieldNames.FIELD_WITH_GROUP ]: {
            type: Errors.REQUIRED,
            text: 'error',
        },
    });
});

it('getFieldError возвращает ошибку для поля', () => {
    const {
        result,
    } = renderHook(() => useFormState<FieldNames, Fields, Errors>({
        initialValues: {
            [ FieldNames.FIELD ]: 2,
            [ FieldNames.FIELD_WITH_GROUP ]: 'lol',
        },
    }));

    const {
        current: {
            getFieldError,
            setFieldError,
        },
    } = result;

    act(() => {
        setFieldError(FieldNames.FIELD_WITH_GROUP, {
            type: Errors.REQUIRED,
            text: 'error',
        });
    });

    expect(getFieldError(FieldNames.FIELD_WITH_GROUP)).toEqual({
        type: Errors.REQUIRED,
        text: 'error',
    });

    expect(getFieldError(FieldNames.FIELD)).toEqual(undefined);
});

it('getTouchedFields возвращает полапаные поля', () => {
    const {
        result: {
            current: {
                getTouchedFields,
                setFieldValue,
            },
        },
    } = renderHook(() => useFormState<FieldNames, Fields, Errors>({
        initialValues: {
            [ FieldNames.FIELD ]: 2,
            [ FieldNames.FIELD_WITH_GROUP ]: 'lol',
        },
    }));

    act(() => {
        setFieldValue(FieldNames.FIELD_WITH_GROUP, 'loooool');
    });

    expect(getTouchedFields()).toEqual([ FieldNames.FIELD_WITH_GROUP ]);
});

it('isFieldTouched возвращает полапаность поля', () => {
    const {
        result: {
            current: {
                isFieldTouched,
                setFieldValue,
            },
        },
    } = renderHook(() => useFormState<FieldNames, Fields, Errors>({
        initialValues: {
            [ FieldNames.FIELD ]: 2,
            [ FieldNames.FIELD_WITH_GROUP ]: 'lol',
        },
    }));

    act(() => {
        setFieldValue(FieldNames.FIELD_WITH_GROUP, 'loooool');
    });

    expect(isFieldTouched(FieldNames.FIELD_WITH_GROUP)).toBe(true);
    expect(isFieldTouched(FieldNames.FIELD)).toBe(false);
});

it('focusField ставит флаг focused и prev focused', () => {
    const {
        result,
    } = renderHook(() => useFormState<FieldNames, Fields, Errors>({
        initialValues: {
            [ FieldNames.FIELD ]: 2,
            [ FieldNames.FIELD_WITH_GROUP ]: 'lol',
        },
    }));

    const {
        current: {
            focusField,
        },
    } = result;

    act(() => {
        focusField(FieldNames.FIELD);
    });

    expect(result.current.state.focused).toEqual(FieldNames.FIELD);
    expect(result.current.state.previousFocused).toEqual(null);

    act(() => {
        // два фокуса подряд быть не должно кроме всяких тыков в саджест
        // потому фокус не должен менять предыдущий фокус
        focusField(FieldNames.FIELD);
        focusField(FieldNames.FIELD_WITH_GROUP);
    });

    expect(result.current.state.focused).toEqual(FieldNames.FIELD_WITH_GROUP);
    expect(result.current.state.previousFocused).toEqual(null);
});

it('blurField ставит флаг focused и prev focused', () => {
    const {
        result,
    } = renderHook(() => useFormState<FieldNames, Fields, Errors>({
        initialValues: {
            [ FieldNames.FIELD ]: 2,
            [ FieldNames.FIELD_WITH_GROUP ]: 'lol',
        },
    }));

    const {
        current: {
            blurField,
            focusField,
        },
    } = result;

    act(() => {
        focusField(FieldNames.FIELD);
        focusField(FieldNames.FIELD_WITH_GROUP);
    });

    act(() => {
        blurField(FieldNames.FIELD_WITH_GROUP);
    });

    expect(result.current.state.focused).toEqual(null);
    expect(result.current.state.previousFocused).toEqual(FieldNames.FIELD_WITH_GROUP);
});

it('isFieldFocused вертает в фокусе ли поле', () => {
    const {
        result,
    } = renderHook(() => useFormState<FieldNames, Fields, Errors>({
        initialValues: {
            [ FieldNames.FIELD ]: 2,
            [ FieldNames.FIELD_WITH_GROUP ]: 'lol',
        },
    }));

    const {
        current: {
            focusField,
            isFieldFocused,
        },
    } = result;

    act(() => {
        focusField(FieldNames.FIELD);
    });

    expect(isFieldFocused(FieldNames.FIELD)).toBe(true);
    expect(isFieldFocused(FieldNames.FIELD_WITH_GROUP)).toBe(false);
});
