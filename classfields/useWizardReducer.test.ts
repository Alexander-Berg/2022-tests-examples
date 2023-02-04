import { renderHook, act } from '@testing-library/react-hooks';

import useWizardReducer, { MAX_FORCE_UPDATE_COUNTER } from './useWizardReducer';

it('startForceUpdate обновляет флаг forceUpdate по кругу', () => {
    const {
        result,
    } = renderHook(() => useWizardReducer({
        forceUpdate: 0,
        currentStepOrder: 0,
    }));

    act(() => {
        result.current.startForceUpdate();
    });

    expect(result.current.state.forceUpdate).toEqual(1);

    let i = MAX_FORCE_UPDATE_COUNTER + 2;

    while (i--) {
        act(() => {
            result.current.startForceUpdate();
        });
    }

    expect(result.current.state.forceUpdate).toEqual(2);
});

it('setCurrentStep обновляет currentStepOrder', () => {
    const {
        result,
    } = renderHook(() => useWizardReducer({
        forceUpdate: 0,
        currentStepOrder: 0,
    }));

    act(() => {
        result.current.setCurrentStep(8);
    });

    expect(result.current.state.currentStepOrder).toEqual(8);
});
