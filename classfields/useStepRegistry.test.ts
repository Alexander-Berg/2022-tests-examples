import { renderHook, act } from '@testing-library/react-hooks';

import stepsRegistryMock from 'auto-core/react/components/common/Wizard/hooks/stepsRegistry.mockchain';
import stepsRegistryStepMock from 'auto-core/react/components/common/Wizard/hooks/stepsRegistryStep.mockchain';

import useStepRegistry from './useStepRegistry';

it('добавляет шаги в отсортированном по order порядке', () => {
    const forceUpdate = jest.fn();

    /* eslint-disable no-unexpected-multiline, @typescript-eslint/indent */
    const stepsRegistry = stepsRegistryMock
        .withNSteps(2)
        [0]
            .withOrder(2)
            .up()
        [1]
            .withOrder(5)
            .up()
        .value();
    /* eslint-enable no-unexpected-multiline, @typescript-eslint/indent */

    const {
        result,
    } = renderHook(() => useStepRegistry(forceUpdate));

    act(() => {
        result.current.registerStep(
            stepsRegistryStepMock.withOrder(5).value(),
        );
        result.current.registerStep(
            stepsRegistryStepMock.withOrder(2).value(),
        );
    });

    expect(result.current.stepsRegistry).toEqual(stepsRegistry);
    expect(forceUpdate).toHaveBeenCalledTimes(2);
});

it('удаляет шаг, оставляя порядок', () => {
    const forceUpdate = jest.fn();

    /* eslint-disable no-unexpected-multiline, @typescript-eslint/indent */
    const stepsRegistry = stepsRegistryMock
        .withNSteps(2)
        [0]
            .withOrder(2)
            .up()
        [1]
            .withOrder(15)
            .up()
        .value();
    /* eslint-enable no-unexpected-multiline, @typescript-eslint/indent */

    const {
        result,
    } = renderHook(() => useStepRegistry(forceUpdate));

    act(() => {
        result.current.registerStep(
            stepsRegistryStepMock.withOrder(5).value(),
        );
        result.current.registerStep(
            stepsRegistryStepMock.withOrder(2).value(),
        );
        result.current.registerStep(
            stepsRegistryStepMock.withOrder(15).value(),
        );
    });

    act(() => {
        result.current.unregisterStep(5);
    });

    expect(result.current.stepsRegistry).toEqual(stepsRegistry);
    expect(forceUpdate).toHaveBeenCalledTimes(4);
});
