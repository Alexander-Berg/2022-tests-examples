import { renderHook } from '@testing-library/react-hooks';

import stepsRegistryMock from 'auto-core/react/components/common/Wizard/hooks/stepsRegistry.mockchain';

import useProgress from './useProgress';

/* eslint-disable no-unexpected-multiline, @typescript-eslint/indent */
const stepsRegistry = stepsRegistryMock
    .withNSteps(3)
    [0]
        .withOrder(0)
        .up()
    [1]
        .withOrder(1)
        .up()
    [2]
        .withOrder(2)
        .up()
    .value();
/* eslint-enable no-unexpected-multiline, @typescript-eslint/indent */

it('возвращает 0, для первого currentStepOrder', () => {
    const {
        result,
    } = renderHook(() => useProgress(0, stepsRegistry));

    expect(result.current()).toEqual(0);
});

it('возвращает 50, для второго currentStepOrder', () => {
    const {
        result,
    } = renderHook(() => useProgress(1, stepsRegistry));

    expect(result.current()).toEqual(50);
});

it('возвращает 100, для третьего currentStepOrder', () => {
    const {
        result,
    } = renderHook(() => useProgress(2, stepsRegistry));

    expect(result.current()).toEqual(100);
});

it('возвращает 100, для второго currentStepOrder при указании maxSteps 2', () => {
    const {
        result,
    } = renderHook(() => useProgress(1, stepsRegistry));

    expect((result.current as (maxSteps: number) => number)(2)).toEqual(100);
});
