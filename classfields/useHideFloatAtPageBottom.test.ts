/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import { renderHook, act } from '@testing-library/react-hooks';

import sleep from 'auto-core/lib/sleep';

import useHideFloatAtPageBottom from './useHideFloatAtPageBottom';

const eventMap: Record<string, EventListenerOrEventListenerObject> = {};
let originalWindowInnerHeight: number;

beforeEach(() => {
    jest.spyOn(global, 'addEventListener').mockImplementation((eventType, callback) => {
        eventMap[eventType] = callback;
    });

    originalWindowInnerHeight = global.innerHeight;

    global.innerHeight = 1000;
    jest.spyOn(document.body, 'scrollHeight', 'get')
        .mockImplementation(() => 2000);
});

afterEach(() => {
    jest.restoreAllMocks();
    global.innerHeight = originalWindowInnerHeight;
});

it('по дефолту вернет true', async() => {
    const { rerender, result } = renderHook(() => useHideFloatAtPageBottom(100));

    expect(result.current).toBe(true);

    await act(() => {
        rerender();
        return sleep(500); // здесь и далее вынужденно ждем из-за дебаунса
    });

    expect(result.current).toBe(true);
});

it('если не доскроллили до конца страницы вернет true', async() => {
    const { rerender, result } = renderHook(() => useHideFloatAtPageBottom(100));

    await act(() => {
        rerender();

        global.scrollY = 250;
        (eventMap.scroll as EventListener)({} as Event);

        return sleep(500);
    });

    expect(result.current).toBe(true);
});

it('если доскроллили до конца страницы с учетом оффсета вернет false', async() => {
    const { rerender, result } = renderHook(() => useHideFloatAtPageBottom(100));

    await act(() => {
        rerender();

        global.scrollY = 950;
        (eventMap.scroll as EventListener)({} as Event);

        return sleep(500);
    });

    expect(result.current).toBe(false);
});

it('если проскроллилил обратно то снова вернет true', async() => {
    const { rerender, result } = renderHook(() => useHideFloatAtPageBottom(100));

    await act(() => {
        rerender();

        global.scrollY = 950;
        (eventMap.scroll as EventListener)({} as Event);

        return sleep(500);
    });

    await act(() => {
        rerender();

        global.scrollY = 500;
        (eventMap.scroll as EventListener)({} as Event);

        return sleep(500);
    });

    expect(result.current).toBe(true);
});
