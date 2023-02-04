jest.mock('auto-core/react/lib/localstorage', () => ({
    getItem: jest.fn(),
}));

import { getItem } from 'auto-core/react/lib/localstorage';

import retrieveDeselectedOptions from './retrieveDeselectedOptions';

const getItemMock = getItem as jest.MockedFunction<typeof getItem>;

it('если в сторадже ничего нет, вернет пустой объект', () => {
    getItemMock.mockReturnValueOnce(null);

    const result = retrieveDeselectedOptions();

    expect(result).toEqual({});
});

it('если в сторадже есть что-то но это не объект, вернет пустой объект', () => {
    getItemMock.mockReturnValueOnce(JSON.stringify(false));

    let result = retrieveDeselectedOptions();
    expect(result).toEqual({});

    getItemMock.mockReturnValueOnce(JSON.stringify([]));

    result = retrieveDeselectedOptions();
    expect(result).toEqual({});
});

it('если в сторадже есть объект, вернет его', () => {
    const obj = { foo: 'bar' };
    getItemMock.mockReturnValueOnce(JSON.stringify(obj));

    const result = retrieveDeselectedOptions();
    expect(result).toEqual(obj);
});
