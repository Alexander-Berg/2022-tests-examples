import { renderHook, act } from '@testing-library/react-hooks';
import type React from 'react';

import useGallery from 'www-mobile/react/components/BigImageGallery/hooks/useGallery';

beforeAll(() => {
    jest.useFakeTimers();
});

afterAll(() => {
    jest.runOnlyPendingTimers();
    jest.useRealTimers();
});

it('переключит на следующий элемент по истечению таймера', () => {
    const {
        result,
    } = render();

    act(() => {
        jest.advanceTimersByTime(5000);
    });
    expect(result.current.slide).toEqual(1);
});

it('правильно переключает слайды вперёд', () => {
    jest.spyOn(global, 'clearTimeout');

    const {
        result,
    } = render();

    act(() => {
        result.current.nextSlide();
    });
    expect(clearTimeout).toHaveBeenCalled();
    expect(result.current.slide).toEqual(1);

    act(() => {
        result.current.nextSlide();
        result.current.nextSlide();
    });
    expect(clearTimeout).toHaveBeenCalled();
    expect(result.current.slide).toEqual(0);
});

it('правильно переключает слайды назад', () => {
    jest.spyOn(global, 'clearTimeout');

    const {
        result,
    } = render();

    act(() => {
        result.current.previousSlide();
    });
    expect(clearTimeout).toHaveBeenCalled();
    expect(result.current.slide).toEqual(2);

    act(() => {
        result.current.previousSlide();
        result.current.previousSlide();
    });
    expect(clearTimeout).toHaveBeenCalled();
    expect(result.current.slide).toEqual(0);
});

it('правильно переключает на определённый слайд', () => {
    jest.spyOn(global, 'clearTimeout');

    const {
        result,
    } = render();

    act(() => {
        result.current.changeSlide({ currentTarget: { getAttribute: () => '2' } } as unknown as React.MouseEvent<HTMLElement>);
    });
    expect(clearTimeout).toHaveBeenCalled();
    expect(result.current.slide).toEqual(2);
});

function render() {
    return renderHook(() => useGallery({ photos: [ {}, {}, {} ] }));
}
