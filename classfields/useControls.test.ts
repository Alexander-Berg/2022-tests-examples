import { renderHook, act } from '@testing-library/react-hooks';

import useControls from './useControls';

it('запоминает и сбрасывает hasControls и вызывает forceUpdate', () => {
    const forceUpdate = jest.fn();

    const {
        rerender,
        result,
    } = renderHook(() => useControls(forceUpdate));

    act(() => {
        result.current.registerControls();
    });

    rerender(true);

    expect(result.current.hasControls).toBe(true);
    expect(forceUpdate).toHaveBeenCalledTimes(1);

    act(() => {
        result.current.unregisterControls();
    });

    rerender(true);

    expect(result.current.hasControls).toBe(false);
    expect(forceUpdate).toHaveBeenCalledTimes(2);
});
