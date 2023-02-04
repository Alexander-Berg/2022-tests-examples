import { renderHook } from '@testing-library/react-hooks';

import creditFormBlocksRegistryBlock from 'auto-core/react/components/common/CreditForm/hooks/CreditForm/creditFormBlocksRegistryBlock.mockchain';
import creditFormBlocksRegistry from 'auto-core/react/components/common/CreditForm/hooks/CreditForm/creditFormBlocksRegistry.mockchain';
import creditFormFieldsRegistryFieldMock from 'auto-core/react/components/common/CreditForm/hooks/CreditFormBlock/creditFormFieldsRegistryField.mock';
import type {
    CreditFormBlocksRegistryBlock,
} from 'auto-core/react/components/common/CreditForm/types';

import useCreditFormHandlers from './useCreditFormHandlers';

describe('validateBlock', () => {
    it('кидает ошибку, если блока нет', async() => {
        const blocksRegistry = creditFormBlocksRegistry
            .withBlocks({
                block1: creditFormBlocksRegistryBlock
                    .withName('block1'),
                block2: creditFormBlocksRegistryBlock
                    .withName('block2'),
            })
            .value();

        const {
            result: {
                current: {
                    validateBlock,
                },
            },
        } = renderHook((blocksRegistry) => useCreditFormHandlers(blocksRegistry), {
            initialProps: blocksRegistry,
        });

        expect.assertions(1);
        await expect(validateBlock('block3')).rejects.toEqual('Can\'t find block named block3');
    });

    it('c silently вызывает validateSilently у блока и возвращает результат', async() => {
        const blocksRegistry = creditFormBlocksRegistry
            .withBlocks({
                block1: creditFormBlocksRegistryBlock
                    .withName('block1'),
                block2: creditFormBlocksRegistryBlock
                    .withName('block2'),
            })
            .value();

        const {
            result: {
                current: {
                    validateBlock,
                },
            },
        } = renderHook((blocksRegistry) => useCreditFormHandlers(blocksRegistry), {
            initialProps: blocksRegistry,
        });

        (blocksRegistry.block2.validateSilently as jest.MockedFunction<CreditFormBlocksRegistryBlock['validateSilently']>)
            .mockImplementationOnce(() => Promise.resolve({
                field1: 'error',
            }));

        expect.assertions(1);
        await expect(validateBlock('block2', true)).resolves.toEqual({
            field1: 'error',
        });
    });

    it('без silently вызывает validate у блока и возвращает результат', async() => {
        const blocksRegistry = creditFormBlocksRegistry
            .withBlocks({
                block1: creditFormBlocksRegistryBlock
                    .withName('block1'),
                block2: creditFormBlocksRegistryBlock
                    .withName('block2'),
            })
            .value();

        const {
            result: {
                current: {
                    validateBlock,
                },
            },
        } = renderHook((blocksRegistry) => useCreditFormHandlers(blocksRegistry), {
            initialProps: blocksRegistry,
        });

        (blocksRegistry.block2.validate as jest.MockedFunction<CreditFormBlocksRegistryBlock['validate']>)
            .mockImplementationOnce(() => Promise.resolve({
                field1: 'error',
            }));

        expect.assertions(1);

        await expect(validateBlock('block2', false)).resolves.toEqual({
            field1: 'error',
        });
    });
});

describe('validateField', () => {
    it('кидает исключение, если поле не найдено', async() => {
        const blocksRegistry = creditFormBlocksRegistry
            .withBlocks({
                block1: creditFormBlocksRegistryBlock
                    .withName('block1'),
                block2: creditFormBlocksRegistryBlock
                    .withName('block2'),
            })
            .value();

        const {
            result: {
                current: {
                    validateField,
                },
            },
        } = renderHook((blocksRegistry) => useCreditFormHandlers(blocksRegistry), {
            initialProps: blocksRegistry,
        });

        expect.assertions(1);

        await expect(validateField('field-some')).rejects.toEqual('Can\'t find field named field-some');
    });

    it('вызывает validateField у найденного блока с полем', async() => {
        const blocksRegistry = creditFormBlocksRegistry
            .withBlocks({
                block1: creditFormBlocksRegistryBlock
                    .withName('block1')
                    .withFields({
                        field1: {
                            ...creditFormFieldsRegistryFieldMock,
                        },
                    }),
                block2: creditFormBlocksRegistryBlock
                    .withName('block2')
                    .withFields({
                        'field-some': {
                            ...creditFormFieldsRegistryFieldMock,
                        },
                    }),
            })
            .value();

        const {
            result: {
                current: {
                    validateField,
                },
            },
        } = renderHook((blocksRegistry) => useCreditFormHandlers(blocksRegistry), {
            initialProps: blocksRegistry,
        });

        expect.assertions(3);

        await expect(validateField('field-some')).resolves.toEqual(true);
        expect(blocksRegistry.block2.validateField).toHaveBeenCalledTimes(1);
        expect(blocksRegistry.block2.validateField).toHaveBeenCalledWith('field-some');
    });
});

it('validateSilentlyAndGetErrors вызывает validateSilently у блоков и возвращает общий набор ошибок', async() => {
    const blocksRegistry = creditFormBlocksRegistry
        .withBlocks({
            block1: creditFormBlocksRegistryBlock
                .withName('block1'),
            block2: creditFormBlocksRegistryBlock
                .withName('block2'),
        })
        .value();

    const {
        result: {
            current: {
                validateSilentlyAndGetErrors,
            },
        },
    } = renderHook((blocksRegistry) => useCreditFormHandlers(blocksRegistry), {
        initialProps: blocksRegistry,
    });

    (blocksRegistry.block1.validateSilently as jest.MockedFunction<CreditFormBlocksRegistryBlock['validateSilently']>)
        .mockImplementationOnce(() => Promise.resolve({
            field1: 'error',
        }));

    (blocksRegistry.block2.validateSilently as jest.MockedFunction<CreditFormBlocksRegistryBlock['validateSilently']>)
        .mockImplementationOnce(() => Promise.resolve({
            field2: 'error',
        }));

    expect.assertions(1);
    await expect(validateSilentlyAndGetErrors()).resolves.toEqual({
        field1: 'error',
        field2: 'error',
    });
});

it('setValue ищет в блоках поле и вызывает у него setValue', () => {
    const blocksRegistry = creditFormBlocksRegistry
        .withBlocks({
            block1: creditFormBlocksRegistryBlock
                .withName('block1')
                .withFields({
                    field1: {
                        ...creditFormFieldsRegistryFieldMock,
                    },
                }),
            block2: creditFormBlocksRegistryBlock
                .withName('block2')
                .withFields({
                    'field-some': {
                        ...creditFormFieldsRegistryFieldMock,
                    },
                }),
        })
        .value();

    const {
        result: {
            current: {
                setValue,
            },
        },
    } = renderHook((blocksRegistry) => useCreditFormHandlers(blocksRegistry), {
        initialProps: blocksRegistry,
    });

    setValue('field-some', 'value');

    expect(blocksRegistry.block2.setValue).toHaveBeenCalledTimes(1);
    expect(blocksRegistry.block2.setValue).toHaveBeenCalledWith('field-some', 'value');
});

it('getValue ищет в блоках поле и вызывает у него getValue', () => {
    const blocksRegistry = creditFormBlocksRegistry
        .withBlocks({
            block1: creditFormBlocksRegistryBlock
                .withName('block1')
                .withFields({
                    field1: {
                        ...creditFormFieldsRegistryFieldMock,
                    },
                }),
            block2: creditFormBlocksRegistryBlock
                .withName('block2')
                .withFields({
                    'field-some': {
                        ...creditFormFieldsRegistryFieldMock,
                    },
                }),
        })
        .value();

    const {
        result: {
            current: {
                getValue,
            },
        },
    } = renderHook((blocksRegistry) => useCreditFormHandlers(blocksRegistry), {
        initialProps: blocksRegistry,
    });

    (blocksRegistry.block2.getValue as jest.MockedFunction<CreditFormBlocksRegistryBlock['getValue']>)
        .mockImplementationOnce(() => 'some value');

    expect(getValue('field-some')).toEqual('some value');
    expect(blocksRegistry.block2.getValue).toHaveBeenCalledWith('field-some');
});

it('getValues вызывает у всех блоков getValues и выдает сморженный результат', () => {
    const blocksRegistry = creditFormBlocksRegistry
        .withBlocks({
            block1: creditFormBlocksRegistryBlock
                .withName('block1'),
            block2: creditFormBlocksRegistryBlock
                .withName('block2'),
        })
        .value();

    const {
        result: {
            current: {
                getValues,
            },
        },
    } = renderHook((blocksRegistry) => useCreditFormHandlers(blocksRegistry), {
        initialProps: blocksRegistry,
    });

    (blocksRegistry.block1.getValues as jest.MockedFunction<CreditFormBlocksRegistryBlock['getValues']>)
        .mockImplementationOnce(() => ({
            field1: 'value',
        }));

    (blocksRegistry.block2.getValues as jest.MockedFunction<CreditFormBlocksRegistryBlock['getValues']>)
        .mockImplementationOnce(() => ({
            field2: 'value',
        }));

    expect.assertions(1);
    expect(getValues()).toEqual({
        field1: 'value',
        field2: 'value',
    });
});

it('getError ищет в блоках поле и вызывает у него getError', () => {
    const blocksRegistry = creditFormBlocksRegistry
        .withBlocks({
            block1: creditFormBlocksRegistryBlock
                .withName('block1')
                .withFields({
                    field1: {
                        ...creditFormFieldsRegistryFieldMock,
                    },
                }),
            block2: creditFormBlocksRegistryBlock
                .withName('block2')
                .withFields({
                    'field-some': {
                        ...creditFormFieldsRegistryFieldMock,
                    },
                }),
        })
        .value();

    const {
        result: {
            current: {
                getError,
            },
        },
    } = renderHook((blocksRegistry) => useCreditFormHandlers(blocksRegistry), {
        initialProps: blocksRegistry,
    });

    (blocksRegistry.block2.getError as jest.MockedFunction<CreditFormBlocksRegistryBlock['getError']>)
        .mockImplementationOnce(() => 'some error');

    expect(getError('field-some')).toEqual('some error');
    expect(blocksRegistry.block2.getError).toHaveBeenCalledTimes(1);
    expect(blocksRegistry.block2.getError).toHaveBeenCalledWith('field-some');
});

describe('getBlockFieldNames', () => {
    it('вызывает у блока getFieldNames, если блок есть', () => {
        const blocksRegistry = creditFormBlocksRegistry
            .withBlocks({
                block1: creditFormBlocksRegistryBlock
                    .withName('block1'),
            })
            .value();

        const {
            result: {
                current: {
                    getBlockFieldNames,
                },
            },
        } = renderHook((blocksRegistry) => useCreditFormHandlers(blocksRegistry), {
            initialProps: blocksRegistry,
        });

        (blocksRegistry.block1.getFieldNames as jest.MockedFunction<CreditFormBlocksRegistryBlock['getFieldNames']>)
            .mockImplementationOnce(() => [ 'someField' ]);

        expect(getBlockFieldNames('block1')).toEqual([ 'someField' ]);
    });

    it('возвращает null, если блока нет', () => {
        const blocksRegistry = creditFormBlocksRegistry
            .withBlocks({
                block1: creditFormBlocksRegistryBlock
                    .withName('block1'),
            })
            .value();

        const {
            result: {
                current: {
                    getBlockFieldNames,
                },
            },
        } = renderHook((blocksRegistry) => useCreditFormHandlers(blocksRegistry), {
            initialProps: blocksRegistry,
        });

        expect(getBlockFieldNames('block2')).toBeNull();
    });
});

it('getErrors вызывает у всех блоков getValues и выдает сморженный результат', () => {
    const blocksRegistry = creditFormBlocksRegistry
        .withBlocks({
            block1: creditFormBlocksRegistryBlock
                .withName('block1'),
            block2: creditFormBlocksRegistryBlock
                .withName('block2'),
        })
        .value();

    const {
        result: {
            current: {
                getErrors,
            },
        },
    } = renderHook((blocksRegistry) => useCreditFormHandlers(blocksRegistry), {
        initialProps: blocksRegistry,
    });

    (blocksRegistry.block1.getErrors as jest.MockedFunction<CreditFormBlocksRegistryBlock['getValues']>)
        .mockImplementationOnce(() => ({
            field1: 'error',
        }));

    (blocksRegistry.block2.getErrors as jest.MockedFunction<CreditFormBlocksRegistryBlock['getValues']>)
        .mockImplementationOnce(() => ({
            field2: 'error',
        }));

    expect(getErrors()).toEqual({
        field1: 'error',
        field2: 'error',
    });
});

it('focusField находит блок с полем и вызывает на нем focusField', () => {
    const blocksRegistry = creditFormBlocksRegistry
        .withBlocks({
            block1: creditFormBlocksRegistryBlock
                .withName('block1')
                .withFields({
                    field1: {
                        ...creditFormFieldsRegistryFieldMock,
                    },
                }),
            block2: creditFormBlocksRegistryBlock
                .withName('block2')
                .withFields({
                    'field-some': {
                        ...creditFormFieldsRegistryFieldMock,
                    },
                }),
        })
        .value();

    const {
        result: {
            current: {
                focusField,
            },
        },
    } = renderHook((blocksRegistry) => useCreditFormHandlers(blocksRegistry), {
        initialProps: blocksRegistry,
    });

    focusField('field-some');

    expect(blocksRegistry.block2.focusField).toHaveBeenCalledTimes(1);
    expect(blocksRegistry.block2.focusField).toHaveBeenCalledWith('field-some');
});

it('focusNextFieldInBlock вызывает на нужном блоке focusNextField', () => {
    const blocksRegistry = creditFormBlocksRegistry
        .withBlocks({
            block1: creditFormBlocksRegistryBlock
                .withName('block1')
                .withFields({
                    field1: {
                        ...creditFormFieldsRegistryFieldMock,
                    },
                }),
            block2: creditFormBlocksRegistryBlock
                .withName('block2')
                .withFields({
                    'field-some': {
                        ...creditFormFieldsRegistryFieldMock,
                    },
                }),
        })
        .value();

    const {
        result: {
            current: {
                focusNextFieldInBlock,
            },
        },
    } = renderHook((blocksRegistry) => useCreditFormHandlers(blocksRegistry), {
        initialProps: blocksRegistry,
    });

    focusNextFieldInBlock('block2');

    expect(blocksRegistry.block2.focusNextField).toHaveBeenCalledTimes(1);
});
