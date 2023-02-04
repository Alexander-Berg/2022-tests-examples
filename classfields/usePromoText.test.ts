import { renderHook } from '@testing-library/react-hooks';

import usePromoText from './usePromoText';

beforeEach(() => {
    jest.spyOn(global.Math, 'random');
});

afterEach(() => {
    jest.restoreAllMocks();
});

it('правильно формирует текст первого типа', () => {
    (global.Math.random as jest.Mock).mockReturnValue(0);
    const { rerender, result } = renderHook(() => usePromoText());

    expect(result.current).toMatchSnapshot();

    // проверяем что при ререндерах ничего не поменяется
    const firstResult = { ...result.current };
    rerender();
    expect(result.current).toEqual(firstResult);
});

it('правильно формирует текст второго типа', () => {
    (global.Math.random as jest.Mock).mockReturnValue(1);
    const { rerender, result } = renderHook(() => usePromoText());

    expect(result.current).toMatchSnapshot();

    // проверяем что при ререндерах ничего не поменяется
    const firstResult = { ...result.current };
    rerender();
    expect(result.current).toEqual(firstResult);
});
