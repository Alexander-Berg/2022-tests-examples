import { renderHook, act } from '@testing-library/react-hooks';

import formFieldRegistryFieldMockChain from 'auto-core/react/components/common/Form/hooks/formFieldRegistryField.mockchain';
import formFieldRegistryMockChain from 'auto-core/react/components/common/Form/hooks/formFieldRegistry.mockchain';

import useFormRegistry from './useFormRegistry';

enum Errors {
    REQUIRED = 'required',
}

it('registerField регистрирует поля и группы', () => {
    enum FieldNames {
        FIELD_WITH_GROUP= 'group.field',
        FIELD = 'field',
    }

    type Fields = {
        [ FieldNames.FIELD_WITH_GROUP ]: string;
        [ FieldNames.FIELD ]: string;
    }

    const {
        result: {
            current: {
                fields,
                groups,
                registerField,
            },
        },
    } = renderHook(() => useFormRegistry<FieldNames, Fields, Errors>());

    const registryMock = formFieldRegistryMockChain<FieldNames, Fields, Errors>()
        .withFields({
            [ FieldNames.FIELD_WITH_GROUP ]: formFieldRegistryFieldMockChain<FieldNames, Fields, Errors>()
                .withName(FieldNames.FIELD_WITH_GROUP),
            [ FieldNames.FIELD ]: formFieldRegistryFieldMockChain<FieldNames, Fields, Errors>()
                .withName(FieldNames.FIELD),
        })
        .withGroups({
            'default': [ FieldNames.FIELD ],
            group: [ FieldNames.FIELD_WITH_GROUP ],
        })
        .value();

    act(() => {
        registerField(
            formFieldRegistryFieldMockChain<FieldNames, Fields, Errors>()
                .withName(FieldNames.FIELD_WITH_GROUP)
                .value(),
        );

        registerField(
            formFieldRegistryFieldMockChain<FieldNames, Fields, Errors>()
                .withName(FieldNames.FIELD)
                .value(),
        );
    });

    expect(groups).toEqual(registryMock.groups);
    expect(Object.keys(fields)).toEqual([ FieldNames.FIELD_WITH_GROUP, FieldNames.FIELD ]);
});

it('unregisterBlock удаляет поля и группы', () => {
    enum FieldNames {
        FIELD_WITH_GROUP= 'group.field',
        FIELD_WITH_GROUP_2 = 'group.field2',
        FIELD_WITH_GROUP_3 = 'group.field3',
        FIELD = 'field',
    }

    type Fields = {
        [ FieldNames.FIELD_WITH_GROUP ]: string;
        [ FieldNames.FIELD_WITH_GROUP_2 ]: string;
        [ FieldNames.FIELD_WITH_GROUP_3 ]: string;
        [ FieldNames.FIELD ]: string;
    }

    const {
        result: {
            current: {
                fields,
                groups,
                registerField,
                unregisterField,
            },
        },
    } = renderHook(() => useFormRegistry<FieldNames, Fields, Errors>());

    const registryMock = formFieldRegistryMockChain<FieldNames, Fields, Errors>()
        .withFields({
            [ FieldNames.FIELD_WITH_GROUP ]: formFieldRegistryFieldMockChain<FieldNames, Fields, Errors>()
                .withName(FieldNames.FIELD_WITH_GROUP),
            [ FieldNames.FIELD_WITH_GROUP_3 ]: formFieldRegistryFieldMockChain<FieldNames, Fields, Errors>()
                .withName(FieldNames.FIELD_WITH_GROUP_3),
            [ FieldNames.FIELD ]: formFieldRegistryFieldMockChain<FieldNames, Fields, Errors>()
                .withName(FieldNames.FIELD),
        })
        .withGroups({
            group: [ FieldNames.FIELD_WITH_GROUP, FieldNames.FIELD_WITH_GROUP_3 ],
        })
        .value();

    act(() => {
        registerField(
            formFieldRegistryFieldMockChain<FieldNames, Fields, Errors>()
                .withName(FieldNames.FIELD_WITH_GROUP)
                .value(),
        );

        registerField(
            formFieldRegistryFieldMockChain<FieldNames, Fields, Errors>()
                .withName(FieldNames.FIELD_WITH_GROUP_2)
                .value(),
        );

        registerField(
            formFieldRegistryFieldMockChain<FieldNames, Fields, Errors>()
                .withName(FieldNames.FIELD_WITH_GROUP_3)
                .value(),
        );

        registerField(
            formFieldRegistryFieldMockChain<FieldNames, Fields, Errors>()
                .withName(FieldNames.FIELD)
                .value(),
        );
    });

    act(() => {
        unregisterField(FieldNames.FIELD);
        unregisterField(FieldNames.FIELD_WITH_GROUP_2);
    });

    expect(groups).toEqual(registryMock.groups);
    expect(Object.keys(fields)).toEqual([ FieldNames.FIELD_WITH_GROUP, FieldNames.FIELD_WITH_GROUP_3 ]);
});

it('getFieldsCount вертает количество полей', () => {
    enum FieldNames {
        FIELD_WITH_GROUP= 'group.field',
        FIELD = 'field',
    }

    type Fields = {
        [ FieldNames.FIELD_WITH_GROUP ]: string;
        [ FieldNames.FIELD ]: string;
    }

    const {
        result: {
            current: {
                getFieldsCount,
                registerField,
            },
        },
    } = renderHook(() => useFormRegistry<FieldNames, Fields, Errors>());

    act(() => {
        registerField(
            formFieldRegistryFieldMockChain<FieldNames, Fields, Errors>()
                .withName(FieldNames.FIELD)
                .value(),
        );

        registerField(
            formFieldRegistryFieldMockChain<FieldNames, Fields, Errors>()
                .withName(FieldNames.FIELD_WITH_GROUP)
                .value(),
        );
    });

    expect(getFieldsCount()).toEqual(2);
});

it('getFieldNames вертает имена полей', () => {
    enum FieldNames {
        FIELD_WITH_GROUP= 'group.field',
        FIELD = 'field',
    }

    type Fields = {
        [ FieldNames.FIELD_WITH_GROUP ]: string;
        [ FieldNames.FIELD ]: string;
    }

    const {
        result: {
            current: {
                getFieldNames,
                registerField,
            },
        },
    } = renderHook(() => useFormRegistry<FieldNames, Fields, Errors>());

    act(() => {
        registerField(
            formFieldRegistryFieldMockChain<FieldNames, Fields, Errors>()
                .withName(FieldNames.FIELD)
                .value(),
        );

        registerField(
            formFieldRegistryFieldMockChain<FieldNames, Fields, Errors>()
                .withName(FieldNames.FIELD_WITH_GROUP)
                .value(),
        );
    });

    expect(getFieldNames()).toEqual([ FieldNames.FIELD, FieldNames.FIELD_WITH_GROUP ]);
});

it('getGroupsCount вертает количество групп', () => {
    enum FieldNames {
        FIELD_WITH_GROUP= 'group.field',
        FIELD = 'field',
    }

    type Fields = {
        [ FieldNames.FIELD_WITH_GROUP ]: string;
        [ FieldNames.FIELD ]: string;
    }

    const {
        result: {
            current: {
                getGroupsCount,
                registerField,
            },
        },
    } = renderHook(() => useFormRegistry<FieldNames, Fields, Errors>());

    act(() => {
        registerField(
            formFieldRegistryFieldMockChain<FieldNames, Fields, Errors>()
                .withName(FieldNames.FIELD)
                .value(),
        );

        registerField(
            formFieldRegistryFieldMockChain<FieldNames, Fields, Errors>()
                .withName(FieldNames.FIELD_WITH_GROUP)
                .value(),
        );
    });

    expect(getGroupsCount()).toEqual(2);
});

it('getGroupNames вертает имена групп', () => {
    enum FieldNames {
        FIELD_WITH_GROUP= 'group.field',
        FIELD = 'field',
    }

    type Fields = {
        [ FieldNames.FIELD_WITH_GROUP ]: string;
        [ FieldNames.FIELD ]: string;
    }

    const {
        result: {
            current: {
                getGroupNames,
                registerField,
            },
        },
    } = renderHook(() => useFormRegistry<FieldNames, Fields, Errors>());

    act(() => {
        registerField(
            formFieldRegistryFieldMockChain<FieldNames, Fields, Errors>()
                .withName(FieldNames.FIELD)
                .value(),
        );

        registerField(
            formFieldRegistryFieldMockChain<FieldNames, Fields, Errors>()
                .withName(FieldNames.FIELD_WITH_GROUP)
                .value(),
        );
    });

    expect(getGroupNames()).toEqual([ 'default', 'group' ]);
});

it('getGroupFieldNames вертает имена полей в группе', () => {
    enum FieldNames {
        FIELD_WITH_GROUP= 'group.field',
        FIELD = 'field',
    }

    type Fields = {
        [ FieldNames.FIELD_WITH_GROUP ]: string;
        [ FieldNames.FIELD ]: string;
    }

    const {
        result: {
            current: {
                getGroupFieldNames,
                registerField,
            },
        },
    } = renderHook(() => useFormRegistry<FieldNames, Fields, Errors>());

    act(() => {
        registerField(
            formFieldRegistryFieldMockChain<FieldNames, Fields, Errors>()
                .withName(FieldNames.FIELD)
                .value(),
        );

        registerField(
            formFieldRegistryFieldMockChain<FieldNames, Fields, Errors>()
                .withName(FieldNames.FIELD_WITH_GROUP)
                .value(),
        );
    });

    expect(getGroupFieldNames('group')).toEqual([ FieldNames.FIELD_WITH_GROUP ]);
});

it('getGroupFieldNames вертает имена полей в дефолтной группе', () => {
    enum FieldNames {
        FIELD_WITH_GROUP= 'group.field',
        FIELD = 'field',
    }

    type Fields = {
        [ FieldNames.FIELD_WITH_GROUP ]: string;
        [ FieldNames.FIELD ]: string;
    }

    const {
        result: {
            current: {
                getGroupFieldNames,
                registerField,
            },
        },
    } = renderHook(() => useFormRegistry<FieldNames, Fields, Errors>());

    act(() => {
        registerField(
            formFieldRegistryFieldMockChain<FieldNames, Fields, Errors>()
                .withName(FieldNames.FIELD)
                .value(),
        );

        registerField(
            formFieldRegistryFieldMockChain<FieldNames, Fields, Errors>()
                .withName(FieldNames.FIELD_WITH_GROUP)
                .value(),
        );
    });

    expect(getGroupFieldNames()).toEqual([ FieldNames.FIELD ]);
});

it('getGroupFieldNames вертает пустой массив для несуществующей группы', () => {
    enum FieldNames {
        FIELD_WITH_GROUP= 'group.field',
        FIELD = 'field',
    }

    type Fields = {
        [ FieldNames.FIELD_WITH_GROUP ]: string;
        [ FieldNames.FIELD ]: string;
    }

    const {
        result: {
            current: {
                getGroupFieldNames,
                registerField,
            },
        },
    } = renderHook(() => useFormRegistry<FieldNames, Fields, Errors>());

    act(() => {
        registerField(
            formFieldRegistryFieldMockChain<FieldNames, Fields, Errors>()
                .withName(FieldNames.FIELD)
                .value(),
        );

        registerField(
            formFieldRegistryFieldMockChain<FieldNames, Fields, Errors>()
                .withName(FieldNames.FIELD_WITH_GROUP)
                .value(),
        );
    });

    expect(getGroupFieldNames('group2')).toEqual([]);
});

it('getGroupFieldCount вертает количество полей в группе', () => {
    enum FieldNames {
        FIELD_WITH_GROUP= 'group.field',
        FIELD = 'field',
    }

    type Fields = {
        [ FieldNames.FIELD_WITH_GROUP ]: string;
        [ FieldNames.FIELD ]: string;
    }

    const {
        result: {
            current: {
                getGroupFieldCount,
                registerField,
            },
        },
    } = renderHook(() => useFormRegistry<FieldNames, Fields, Errors>());

    act(() => {
        registerField(
            formFieldRegistryFieldMockChain<FieldNames, Fields, Errors>()
                .withName(FieldNames.FIELD)
                .value(),
        );

        registerField(
            formFieldRegistryFieldMockChain<FieldNames, Fields, Errors>()
                .withName(FieldNames.FIELD_WITH_GROUP)
                .value(),
        );
    });

    expect(getGroupFieldCount('group')).toEqual(1);
});
