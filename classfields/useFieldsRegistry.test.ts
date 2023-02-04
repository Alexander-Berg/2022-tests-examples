import { renderHook, act } from '@testing-library/react-hooks';

import useFieldsRegistry from './useFieldsRegistry';
import baseFieldConfig from './creditFormFieldsRegistryField.mock';

it('добавляет поля в нужном порядке', () => {
    const {
        result: {
            current: {
                fieldsRegistry,
                registerField,
            },
        },
    } = renderHook(() => useFieldsRegistry());

    act(() => {
        registerField({
            ...baseFieldConfig,
            name: 'field1',
        });
    });

    act(() => {
        registerField({
            ...baseFieldConfig,
            name: 'field2',
        });
    });

    act(() => {
        registerField({
            ...baseFieldConfig,
            name: 'field3',
        });
    });

    expect(Object.keys(fieldsRegistry)).toEqual([ 'field1', 'field2', 'field3' ]);
});

it('удаляет поля', () => {
    const {
        result: {
            current: {
                fieldsRegistry,
                registerField,
                unregisterField,
            },
        },
    } = renderHook(() => useFieldsRegistry());

    act(() => {
        registerField({
            ...baseFieldConfig,
            name: 'field1',
        });
    });

    act(() => {
        registerField({
            ...baseFieldConfig,
            name: 'field2',
        });
    });

    act(() => {
        registerField({
            ...baseFieldConfig,
            name: 'field3',
        });
    });

    act(() => {
        unregisterField('field2');
    });

    expect(Object.keys(fieldsRegistry)).toEqual([ 'field1', 'field3' ]);
});
