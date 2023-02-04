const MockChain = require('./MockChain');
const MockChainContext = require('./MockChainContext');

function isContext(context) {
    return context instanceof MockChainContext;
}

it('value по умолчанию выдает дефолтные данные (клон)', () => {
    const data = {
        someKey: 1
    };

    const mock = new MockChain(data);
    const mockData = mock.value();

    expect(mockData).toEqual(data);
    expect(mockData === data).toBe(false);
});

it('value по умолчанию выдает дефолтные данные (клон), если на входе функция', () => {
    const data = {
        someKey: 1
    };

    const mock = new MockChain(() => data);
    const mockData = mock.value();

    expect(mockData).toEqual(data);
    expect(mockData === data).toBe(false);
});

it('registerMock правильно именует свойство мока, которое возвращает контекст запуска', () => {
    const mock = new MockChain();

    mock.registerMock('someVariantForMock', () => {});

    const context = mock.someVariantForMock();

    expect(isContext(context)).toBe(true);
});

it('в вариант передаются текущие данные мока и аргументы запуска', () => {
    const initialData = {
        data: 'initialData'
    };

    const runnerArgs = [
        'arg1',
        'arg2'
    ];

    const mock = new MockChain(initialData);

    const variantRunner = jest.fn((data, ...args) => {
        expect(data).toEqual(initialData);
        expect(args).toEqual(runnerArgs);
    });

    mock.registerMock('runner', variantRunner);

    mock.runner(...runnerArgs);

    expect(variantRunner).toHaveBeenCalled();
});

it('вариант дополняет данные контекста мока', () => {
    const initialData = {
        mockDefaultData: 'data',
    };

    const mock = new MockChain(initialData);

    const variantData = {
        variant: 'data',
    };

    mock.registerMock('addData', (data) => {
        return {
            ...data,
            ...variantData,
        }
    });

    expect(mock.addData().value()).toEqual({
        ...initialData,
        ...variantData,
    });
});

it('вариант изменяет данные контекста мока', () => {
    const initialData = {
        mockDefaultData: 'data',
    };

    const mock = new MockChain(initialData);

    const variantData = {
        variant: 'data',
    };

    mock.registerMock('addData', (data) => {
        return {
            ...variantData,
        }
    });

    expect(mock.addData().value()).toEqual({
        ...variantData,
    });
});

it('вариант возвращает тот же контекст, из которого вызван', () => {
    const mock = new MockChain();

    mock.registerMock('runner1', (data) => {
        return {
            ...data,
            runner1: 1,
        };
    });

    mock.registerMock('runner2', (data) => {
        return {
            ...data,
            runner2: 2,
        };
    });

    const firstContext = mock.runner1();
    const secondContext = firstContext.runner2();

    expect(secondContext === firstContext).toBe(true);
    expect(secondContext.value()).toEqual({
        runner1: 1,
        runner2: 2,
    });
});

it('новый вызов создает новую цепочку', () => {
    const mock = new MockChain();

    mock.registerMock('runner1', (data) => {
        return {
            ...data,
            runner1: 1,
        };
    });

    mock.registerMock('runner2', (data) => {
        return {
            ...data,
            runner2: 2,
        };
    });

    const firstContext = mock.runner1();
    const secondContext = mock.runner2();

    expect(secondContext === firstContext).toBe(false);
    expect(firstContext.value()).toEqual({
        runner1: 1,
    });
    expect(secondContext.value()).toEqual({
        runner2: 2,
    });
});

it('вызов варианта не изменяет дефолтные данные', () => {
    const initialData = {
        initial: 'data'
    };

    const mock = new MockChain(initialData);

    mock.registerMock('runner1', (data) => {
        return {
            ...data,
            runner1: 1,
        };
    });

    mock.runner1();

    expect(mock.value()).toEqual(initialData);
});

it('регистрация подмока через конструктор (Object)', () => {
    const subMock = new MockChain();

    subMock.registerMock('subProp', () => {});

    const mock = new MockChain({
        prop: subMock,
    });

    const subContext = mock.prop;

    expect(isContext(subContext)).toBe(true);
    expect('subProp' in subContext).toBe(true);
});

it('регистрация подмока через конструктор (Array)', () => {
    const subMock = new MockChain([]);

    subMock.registerMock('subProp', () => {});

    const mock = new MockChain({
        prop: subMock,
    });

    const subContext = mock.prop;

    expect(isContext(subContext)).toBe(true);
    expect('subProp' in subContext).toBe(true);
});

it('регистрация подмока через registerMock', () => {
    const subMock = new MockChain();

    subMock.registerMock('subProp', () => {});

    const mock = new MockChain();

    mock.registerMock('prop', subMock);

    const subContext = mock.prop;

    expect(isContext(subContext)).toBe(true);
    expect('subProp' in subContext).toBe(true);

});

it('регистрация подмока через возвращаемые данные из registerMock', () => {
    const subMock = new MockChain([]);

    subMock.registerMock('subPropFromSubMock', () => {});

    const mock = new MockChain();

    mock.registerMock('withSubProp', (data) => {
        return {
            ...data,
            subProp: {
                subMock,
            }
        }
    });

    const subContext = mock
        .withSubProp()
        .subProp
            .subMock;

    expect(isContext(subContext)).toBe(true);
    expect('subPropFromSubMock' in subContext).toBe(true);

});

it('вложенный контекст дает вернуться выше в цепочке', () => {
    const subMock = new MockChain([]);

    subMock.registerMock('subProp', () => {});

    const mock = new MockChain();

    mock.registerMock('selfProp', () => {});
    mock.registerMock('withSubProp', (data) => {
        return {
            ...data,
            subProp: {
                subProp2: {
                    subMock
                }
            }
        }
    });

    const context = mock.selfProp();
    const subContext = context
        .withSubProp()
        .subProp
            .subProp2
                .subMock;
    const upperContext = subContext.up().up().up();

    expect(isContext(upperContext)).toBe(true);
    expect(upperContext === context).toBe(true);
});

it('данные собираются с вложенных контекстов', () => {
    const subMock = new MockChain({
        subMockData: 2,
    });

    subMock.registerMock('subProp', (data) => {
        return {
            ...data,
            subProp: 3,
        }
    });

    const mock = new MockChain({
        mockData: 1,
    });

    mock.registerMock('prop', (data) => {
        return {
            ...data,
            prop: 1,
        }
    });

    mock.registerMock('withSubMock', (data) => {
        return {
            ...data,
            subProp: {
                subMock,
            },
        }
    });

    const data = mock
        .withSubMock()
            .subProp
                .subMock
                    .subProp()
                    .up()
            .up()
        .value();

    expect(data).toEqual({
        mockData: 1,
        subProp: {
            subMock: {
                subMockData: 2,
                subProp: 3,
            },
        },
    });
});

it('данные собираются с вложенных контекстов без обращения к вложенному контексту', () => {
    const subMock = new MockChain({
        subMockData: 2,
    });

    subMock.registerMock('subProp', (data) => {
        return {
            ...data,
            subProp: 3,
        }
    });

    const mock = new MockChain({
        mockData: 1,
    });

    mock.registerMock('prop', (data) => {
        return {
            ...data,
            prop: 1,
        }
    });

    mock.registerMock('subMock', subMock);

    const data = mock
        .prop()
        .value();

    expect(data).toEqual({
        mockData: 1,
        prop: 1,
        subMock: {
            subMockData: 2,
        }
    });
});

it('данные подмока собираются в дефолтные', () => {
    const subMock = new MockChain({
        subMockData: 2,
    });

    const mock = new MockChain({
        mockData: 1,
    });

    mock.registerMock('subMock', subMock);

    const data = mock.value();

    expect(data).toEqual({
        mockData: 1,
        subMock: {
            subMockData: 2,
        },
    });
});

it('если мокаем массив, его элементом может быть мок, создающий отдельный контекст и геттеры', () => {
    const subMock = new MockChain({
        subMockData: 2,
    });

    subMock.registerMock('subProp', (data) => {
        return {
            ...data,
            subProp: 3,
        }
    });

    const mock = new MockChain([
        subMock,
    ]);

    mock.registerMock('prop', (data) => {
        return [
            ...data,
            [2],
        ]
    });

    const subContext = mock[0];

    expect(isContext(subContext)).toBe(true);

    const data = subContext
        .subProp()
        .up()
        .prop()
        .value();

    expect(data).toEqual([
        {
            subMockData: 2,
            subProp: 3,
        },
        [2],
    ]);
});

it('если мокаем массив, варианты должны корректно менять исходный массив', () => {
    const mock = new MockChain([
        1,
        2,
        3,
        4,
    ]);

    mock.registerMock('withOtherValues', (data) => {
        return [
            data[0],
            data[2],
            5,
        ]
    });

    const data = mock
        .withOtherValues()
        .value();

    expect(data).toEqual([
        1,
        3,
        5,
    ]);

    expect(data.length).toEqual(3);
});

it('не падает, если в данных есть null', () => {
    expect(() => {
        const mock = new MockChain({
            nullData: null,
        });

        mock.nullData;
    }).not.toThrow();

    expect(() => {
        const mock = new MockChain({
            data: {
                nullData: null,
            }
        });

        mock.data.nullData;
    }).not.toThrow();
});

it('заполняет структуру моков по json и создает новую цепочку', () => {
    const level3Mock = new MockChain({
        prop3: null,
    });

    const level2Mock = new MockChain({
        prop2: null,
        subProp2: level3Mock,
    });

    const level1Mock = new MockChain([
        {
            prop: null,
            subProp: level2Mock,
        },
        {
            prop: null,
        }
    ]);

    const oldValue = level1Mock.value();

    const newData = [
        {
            prop: 'value',
            subProp: {
                prop2: 'value2',
                subProp2: {
                    prop3: 'value3',
                },
            },
        },
        {
            prop: 'value',
        },
    ];

    const newValue = level1Mock
        .fillByProps(newData)
        .value();

    expect(newValue).toEqual(newData);
    expect(level1Mock.value()).toEqual(oldValue);
});
