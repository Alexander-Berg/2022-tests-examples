/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import { renderHook, act } from '@testing-library/react-hooks';

import useSessionStorage from './useSessionStorage';

const MOCK_KEY = 'useSessionStorageMockMag';

beforeEach(() => {
    sessionStorage.clear();

    jest.clearAllMocks();
});

it('ничего не возвращает, если не найдено значение', () => {
    const { result } = renderHook(() => useSessionStorage(MOCK_KEY));

    expect(result.current.value).toBeNull();
});

it('возвращает значение, если оно уже есть в хранилище', () => {
    const MOCK_VALUE = 'hello';
    sessionStorage.setItem(MOCK_KEY, MOCK_VALUE);

    const { result } = renderHook(() => useSessionStorage(MOCK_KEY));

    expect(result.current.value).toBe(MOCK_VALUE);
});

it('возвращает дефолтное значение', () => {
    const DEFAULT_VALUE = true;

    const { result } = renderHook(() => useSessionStorage(MOCK_KEY, DEFAULT_VALUE));

    expect(result.current.value).toBe(DEFAULT_VALUE);
    expect(localStorage.setItem).not.toHaveBeenLastCalledWith(MOCK_KEY, DEFAULT_VALUE);
});

it('меняет значение в хранилище', () => {
    const { result } = renderHook(() => useSessionStorage(MOCK_KEY));

    const newValueForStorage = { foo: 'bar' };
    act(() => {
        result.current.setValue(newValueForStorage);
    });

    expect(result.current.value).toBe(newValueForStorage);
    expect(sessionStorage.setItem).toHaveBeenLastCalledWith(
        MOCK_KEY,
        JSON.stringify(newValueForStorage),
    );
});

it('подписывается на хранилище для изменения значения', () => {
    const eventMap: Record<string, any> = {};

    jest.spyOn(global, 'addEventListener').mockImplementation((event, cb) => {
        eventMap[event] = cb;
    });

    const { result } = renderHook(() => useSessionStorage(MOCK_KEY));

    expect(result.current.value).toBeNull();

    const newValueForStorage = { foo: 'bar' };
    act(() => {
        eventMap.storage({
            storageArea: sessionStorage,
            key: MOCK_KEY,
            newValue: newValueForStorage,
        });
        eventMap.storage({
            storageArea: localStorage,
            key: 'ignoreLocalStorage',
            newValue: true,
        });
    });

    expect(result.current.value).toBe(newValueForStorage);
});
