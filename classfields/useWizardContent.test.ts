import { renderHook } from '@testing-library/react-hooks';

import useWizardContent from './useWizardContent';

it('вызывает registerContent на первом рендере', () => {
    const registerContent = jest.fn();
    const unregisterContent = jest.fn();

    const {
        rerender,
        result,
    } = renderHook(() => useWizardContent({
        registerContent,
        unregisterContent,
        scrollOnNextStep: true,
    }));

    // регистрация до маунта
    expect(registerContent).toHaveBeenCalledTimes(1);
    expect(registerContent).toHaveBeenLastCalledWith({
        scrollOnNextStep: true,
        contentRef: result.current.contentRef,
    });

    rerender({
        registerContent,
        unregisterContent,
        scrollOnNextStep: false,
    });

    // не вызывает на ререндере
    expect(registerContent).toHaveBeenCalledTimes(1);
});

it('вызывает unregisterContent на unmount', () => {
    const registerContent = jest.fn();
    const unregisterContent = jest.fn();

    const {
        rerender,
        unmount,
    } = renderHook(() => useWizardContent({
        registerContent,
        unregisterContent,
        scrollOnNextStep: true,
    }));

    expect(unregisterContent).toHaveBeenCalledTimes(0);

    rerender({
        registerContent,
        unregisterContent,
        scrollOnNextStep: false,
    });

    expect(unregisterContent).toHaveBeenCalledTimes(0);

    unmount();

    expect(unregisterContent).toHaveBeenCalledTimes(1);
});
