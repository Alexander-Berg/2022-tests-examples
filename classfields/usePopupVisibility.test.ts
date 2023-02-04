import { renderHook, act } from '@testing-library/react-hooks';
import { fireEvent } from '@testing-library/react';

import { usePopupVisibility, OPEN_POPUP_WITH_DELAY_WAIT, CLOSE_POPUP_WAIT } from './usePopupVisibility';

describe('usePopupVisibility', () => {
    beforeEach(() => {
        jest.useFakeTimers();
    });
    it('рендерит хук в активном состоянии, скроллит вниз, попап не отображается, после скролла по таймауту показывается попап', () => {
        const setPopupVisiblity = jest.fn();
        renderHook(() => usePopupVisibility({ setPopupVisiblity, isActive: true }));
        fireEvent.scroll(window, { target: { scrollY: 300 } });

        expect(setPopupVisiblity).toHaveBeenCalledWith(false);
        jest.advanceTimersByTime(OPEN_POPUP_WITH_DELAY_WAIT);
        expect(setPopupVisiblity).toHaveBeenCalledWith(true);
    });

    it('рендерит хук в активном состоянии, скроллит вниз, затем наверх, после скролла вверх попап отображается', () => {
        const setPopupVisiblity = jest.fn();
        renderHook(() => usePopupVisibility({ setPopupVisiblity, isActive: true }));
        fireEvent.scroll(window, { target: { scrollY: 300 } });

        expect(setPopupVisiblity).toHaveBeenCalledWith(false);

        fireEvent.scroll(window, { target: { scrollY: -200 } });

        expect(setPopupVisiblity).toHaveBeenLastCalledWith(true);
    });

    it('рендерит хук в активном состоянии, скроллит вниз, затем наверх, попап скрывается по таймауту', () => {
        const setPopupVisiblity = jest.fn();
        renderHook(() => usePopupVisibility({ setPopupVisiblity, isActive: true }));
        fireEvent.scroll(window, { target: { scrollY: 300 } });

        expect(setPopupVisiblity).toHaveBeenNthCalledWith(1, false);

        fireEvent.scroll(window, { target: { scrollY: -200 } });

        expect(setPopupVisiblity).toHaveBeenNthCalledWith(2, true);

        act(() => {
            jest.advanceTimersByTime(CLOSE_POPUP_WAIT);
        });

        expect(setPopupVisiblity).toHaveBeenNthCalledWith(3, false);
    });

    it('рендерит хук в активном состоянии, скроллит вниз, затем наверх, попап не скрывается по таймауту, так как таймер сбрасывается', () => {
        const setPopupVisiblity = jest.fn();
        const { result } = renderHook(() => usePopupVisibility({ setPopupVisiblity, isActive: true }));
        fireEvent.scroll(window, { target: { scrollY: 300 } });

        expect(setPopupVisiblity).toHaveBeenNthCalledWith(1, false);

        fireEvent.scroll(window, { target: { scrollY: -200 } });

        expect(setPopupVisiblity).toHaveBeenNthCalledWith(2, true);

        result.current.stopClosePopupTimer();

        act(() => {
            jest.advanceTimersByTime(CLOSE_POPUP_WAIT);
        });

        expect(setPopupVisiblity).not.toHaveBeenNthCalledWith(3, false);
    });

    it('рендерит хук в активном состоянии, скроллит вниз, затем наверх, попап скрывается принудительно', () => {
        const setPopupVisiblity = jest.fn();
        const { result } = renderHook(() => usePopupVisibility({ setPopupVisiblity, isActive: true }));
        fireEvent.scroll(window, { target: { scrollY: 300 } });

        expect(setPopupVisiblity).toHaveBeenNthCalledWith(1, false);

        fireEvent.scroll(window, { target: { scrollY: -200 } });

        expect(setPopupVisiblity).toHaveBeenNthCalledWith(2, true);

        result.current.closePopupAndRemoveEventListner();

        expect(setPopupVisiblity).toHaveBeenNthCalledWith(3, false);
    });

    it('рендерит хук в активном состоянии, скроллит вниз, затем наверх, попап скрывается при изменении props isActive', async() => {
        const setPopupVisiblity = jest.fn();
        let props = { setPopupVisiblity, isActive: true };

        const { rerender } = renderHook(() => usePopupVisibility(props));
        fireEvent.scroll(window, { target: { scrollY: 300 } });

        expect(setPopupVisiblity).toHaveBeenNthCalledWith(1, false);

        fireEvent.scroll(window, { target: { scrollY: -200 } });

        expect(setPopupVisiblity).toHaveBeenNthCalledWith(2, true);

        props = { setPopupVisiblity, isActive: false };

        rerender();

        expect(setPopupVisiblity).toHaveBeenNthCalledWith(3, false);
    });

    it('переключение isActive не влияет на время обязательного закрытия попапа', async() => {
        const setPopupVisiblity = jest.fn();
        let props = { setPopupVisiblity, isActive: true };

        const { rerender } = renderHook(() => usePopupVisibility(props));

        fireEvent.scroll(window, { target: { scrollY: 300 } });

        // при скроле вниз попап скрывается
        expect(setPopupVisiblity).toHaveBeenNthCalledWith(1, false);

        jest.advanceTimersByTime(OPEN_POPUP_WITH_DELAY_WAIT);

        // после окончания скролла через OPEN_POPUP_WITH_DELAY_WAIT попап открывается
        expect(setPopupVisiblity).toHaveBeenNthCalledWith(2, true);

        // usePopupVisibility выключается
        props = { setPopupVisiblity, isActive: false };
        rerender();

        // попап закрыт
        expect(setPopupVisiblity).toHaveBeenNthCalledWith(3, false);

        // usePopupVisibility актирвируется
        props = { setPopupVisiblity, isActive: true };
        rerender();

        fireEvent.scroll(window, { target: { scrollY: 350 } });

        // при скроле вниз попап закрывается
        expect(setPopupVisiblity).toHaveBeenNthCalledWith(4, false);

        jest.advanceTimersByTime(OPEN_POPUP_WITH_DELAY_WAIT);

        // после окончания скролла через OPEN_POPUP_WITH_DELAY_WAIT попап открывается
        expect(setPopupVisiblity).toHaveBeenNthCalledWith(5, true);

        jest.advanceTimersByTime(CLOSE_POPUP_WAIT - 2 * OPEN_POPUP_WITH_DELAY_WAIT);

        // попап закрывается CLOSE_POPUP_WAIT - 2 * OPEN_POPUP_WITH_DELAY_WAIT (время, которое прошло после остановки скролла)
        expect(setPopupVisiblity).toHaveBeenNthCalledWith(6, false);
    });
});
