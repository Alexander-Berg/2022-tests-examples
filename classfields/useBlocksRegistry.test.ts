import { renderHook, act } from '@testing-library/react-hooks';

import creditFormBlocksRegistryBlock from 'auto-core/react/components/common/CreditForm/hooks/CreditForm/creditFormBlocksRegistryBlock.mockchain';
import creditFormBlocksRegistry from 'auto-core/react/components/common/CreditForm/hooks/CreditForm/creditFormBlocksRegistry.mockchain';
import creditFormFieldsRegistryFieldMock from 'auto-core/react/components/common/CreditForm/hooks/CreditFormBlock/creditFormFieldsRegistryField.mock';
import type {
    CreditFormBlocksRegistryBlock,
} from 'auto-core/react/components/common/CreditForm/types';

import useBlocksRegistry from './useBlocksRegistry';

it('registerBlock регистрирует блоки', () => {
    const {
        result: {
            current: {
                blocksRegistry,
                registerBlock,
            },
        },
    } = renderHook(() => useBlocksRegistry());

    const blocksRegistryMock = creditFormBlocksRegistry
        .withBlocks({
            block1: creditFormBlocksRegistryBlock
                .withName('block1'),
            block2: creditFormBlocksRegistryBlock
                .withName('block2'),
        })
        .value();

    act(() => {
        registerBlock(
            creditFormBlocksRegistryBlock
                .withName('block1')
                .value(),
        );

        registerBlock(
            creditFormBlocksRegistryBlock
                .withName('block2')
                .value(),
        );
    });

    expect(blocksRegistry).toEqual(blocksRegistryMock);
});

it('unregisterBlock выкидывает блоки', () => {
    const {
        result: {
            current: {
                blocksRegistry,
                registerBlock,
                unregisterBlock,
            },
        },
    } = renderHook(() => useBlocksRegistry());

    const blocksRegistryMock = creditFormBlocksRegistry
        .withBlocks({
            block1: creditFormBlocksRegistryBlock
                .withName('block1'),
            block3: creditFormBlocksRegistryBlock
                .withName('block3'),
        })
        .value();

    act(() => {
        registerBlock(
            creditFormBlocksRegistryBlock
                .withName('block1')
                .value(),
        );

        registerBlock(
            creditFormBlocksRegistryBlock
                .withName('block2')
                .value(),
        );

        registerBlock(
            creditFormBlocksRegistryBlock
                .withName('block3')
                .value(),
        );

        unregisterBlock('block2');
    });

    expect(blocksRegistry).toEqual(blocksRegistryMock);
});

describe('getBlockFieldNames', () => {
    it('вызывает getFieldNames у нужного блока, если он есть', () => {
        const {
            result: {
                current: {
                    getBlockFieldNames,
                    registerBlock,
                },
            },
        } = renderHook(() => useBlocksRegistry());

        const block2Mock = creditFormBlocksRegistryBlock
            .withName('block2')
            .value();

        act(() => {
            registerBlock(
                creditFormBlocksRegistryBlock
                    .withName('block1')
                    .value(),
            );
            registerBlock(
                block2Mock,
            );
        });

        (block2Mock.getFieldNames as jest.MockedFunction<CreditFormBlocksRegistryBlock['getFieldNames']>).mockImplementationOnce(() => [ 'field1' ]);

        expect(getBlockFieldNames('block2')).toEqual([ 'field1' ]);

    });

    it('возвращает null, если запрашиваемого блока нет', () => {
        const {
            result: {
                current: {
                    getBlockFieldNames,
                    registerBlock,
                },
            },
        } = renderHook(() => useBlocksRegistry());

        act(() => {
            registerBlock(
                creditFormBlocksRegistryBlock
                    .withName('block1')
                    .value(),
            );
        });

        expect(getBlockFieldNames('block2')).toBeNull();
    });
});
describe('getFieldsInBlockLength', () => {
    it('возвращает количество полей у нужного блока, если он есть', () => {
        const {
            result: {
                current: {
                    getFieldsInBlockLength,
                    registerBlock,
                },
            },
        } = renderHook(() => useBlocksRegistry());

        const block2Mock = creditFormBlocksRegistryBlock
            .withName('block2')
            .withFields({
                field1: {
                    ...creditFormFieldsRegistryFieldMock,
                },
                field2: {
                    ...creditFormFieldsRegistryFieldMock,
                },
            })
            .value();

        act(() => {
            registerBlock(
                creditFormBlocksRegistryBlock
                    .withName('block1')
                    .value(),
            );
            registerBlock(
                block2Mock,
            );
        });

        expect(getFieldsInBlockLength('block2')).toEqual(2);
    });

    it('возвращает 0, если запрашиваемого блока нет', () => {
        const {
            result: {
                current: {
                    getFieldsInBlockLength,
                    registerBlock,
                },
            },
        } = renderHook(() => useBlocksRegistry());

        act(() => {
            registerBlock(
                creditFormBlocksRegistryBlock
                    .withName('block1')
                    .value(),
            );
        });

        expect(getFieldsInBlockLength('block2')).toEqual(0);
    });
});

it('getBlockNames возвращает имена зарегистрированных блоков', () => {
    const {
        result: {
            current: {
                getBlockNames,
                registerBlock,
            },
        },
    } = renderHook(() => useBlocksRegistry());

    act(() => {
        registerBlock(
            creditFormBlocksRegistryBlock
                .withName('block1')
                .value(),
        );
        registerBlock(
            creditFormBlocksRegistryBlock
                .withName('block2')
                .value(),
        );
    });

    expect(getBlockNames()).toEqual([ 'block1', 'block2' ]);
});
