import { renderHook, act } from '@testing-library/react-hooks';

import type { UseWizardReducerReturnType } from 'auto-core/react/components/common/Wizard/hooks/useWizardReducer';
import type { WizardConfig } from 'auto-core/react/components/common/Wizard/types';
import stepsRegistryMock from 'auto-core/react/components/common/Wizard/hooks/stepsRegistry.mockchain';

import useSteps from './useSteps';

/* eslint-disable no-unexpected-multiline, @typescript-eslint/indent */
const stepsRegistry = stepsRegistryMock
    .withNSteps(3)
    [0]
        .withOrder(1)
        .up()
    [1]
        .withOrder(2)
        .up()
    [2]
        .withOrder(3)
        .up()
    .value();
/* eslint-enable no-unexpected-multiline, @typescript-eslint/indent */

let runOrder: (fnName: string) => void;
let setCurrentStep: UseWizardReducerReturnType['setCurrentStep'];
let onNext: WizardConfig['onNext'];
let onAfterChangeStep: WizardConfig['onAfterChangeStep'];
let onFinish: WizardConfig['onFinish'];
let finishOperation: () => Promise<any>;
let operation: () => Promise<any>;

beforeEach(() => {
    runOrder = jest.fn();

    setCurrentStep = jest.fn(() => {
        runOrder('setCurrentStep');
    });
    onNext = jest.fn(() => {
        runOrder('onNext');
    });
    onAfterChangeStep = jest.fn(() => {
        runOrder('onAfterChangeStep');
    });
    onFinish = jest.fn(() => {
        runOrder('onFinish');
    });
    operation = jest.fn(() => {
        runOrder('operation');

        return Promise.resolve();
    });
    finishOperation = jest.fn(() => {
        runOrder('finishOperation');

        return Promise.resolve();
    });
});

describe('goToNextStep', () => {
    it('на не последнем шаге вызывает operation и onNext и setCurrentStep', async() => {
        const {
            result,
        } = renderHook(() => useSteps(
            1,
            stepsRegistry,
            setCurrentStep,
            false,
            onNext,
            onAfterChangeStep,
            onFinish,
        ));

        await act(async() => {
            await result.current.goToNextStep(operation);
        });

        expect(runOrder).toHaveBeenNthCalledWith(1, 'operation');
        expect(runOrder).toHaveBeenNthCalledWith(2, 'onNext');
        expect(runOrder).toHaveBeenNthCalledWith(3, 'setCurrentStep');
        expect(runOrder).toHaveBeenNthCalledWith(4, 'onAfterChangeStep');
        expect(setCurrentStep).toHaveBeenCalledWith(2);
    });

    it('на последнем шаге вызывает operation и onNext и finishOperation и onFinish и setCurrentStep', async() => {
        const {
            result,
        } = renderHook(() => useSteps(
            3,
            stepsRegistry,
            setCurrentStep,
            false,
            onNext,
            onAfterChangeStep,
            onFinish,
        ));

        await act(async() => {
            await result.current.goToNextStep(operation, finishOperation);
        });

        expect(runOrder).toHaveBeenNthCalledWith(1, 'operation');
        expect(runOrder).toHaveBeenNthCalledWith(2, 'onNext');
        expect(runOrder).toHaveBeenNthCalledWith(3, 'finishOperation');
        expect(runOrder).toHaveBeenNthCalledWith(4, 'onFinish');
        expect(runOrder).toHaveBeenNthCalledWith(5, 'setCurrentStep');
        expect(runOrder).toHaveBeenNthCalledWith(6, 'onAfterChangeStep');
        expect(setCurrentStep).toHaveBeenCalledWith(3);
    });

    it('на не последнем шаге если ничего не передано, вызывает переключение шага', async() => {
        const {
            result,
        } = renderHook(() => useSteps(
            1,
            stepsRegistry,
            setCurrentStep,
            false,
        ));

        await act(async() => {
            await result.current.goToNextStep();
        });

        expect(runOrder).toHaveBeenNthCalledWith(1, 'setCurrentStep');
        expect(setCurrentStep).toHaveBeenCalledWith(2);
    });

    it('на последнем шаге если ничего не передано, вызывает переключение шага', async() => {
        const {
            result,
        } = renderHook(() => useSteps(
            3,
            stepsRegistry,
            setCurrentStep,
            false,
        ));

        await act(async() => {
            await result.current.goToNextStep();
        });

        expect(runOrder).toHaveBeenNthCalledWith(1, 'setCurrentStep');
        expect(setCurrentStep).toHaveBeenCalledWith(3);
    });

    it('на не последнем шаге если передан только operation, вызывает operation и переключение шага', async() => {
        const {
            result,
        } = renderHook(() => useSteps(
            1,
            stepsRegistry,
            setCurrentStep,
            false,
        ));

        await act(async() => {
            await result.current.goToNextStep(operation);
        });

        expect(runOrder).toHaveBeenNthCalledWith(1, 'operation');
        expect(runOrder).toHaveBeenNthCalledWith(2, 'setCurrentStep');
        expect(setCurrentStep).toHaveBeenCalledWith(2);
    });

    it('на последнем шаге если передан только operation, вызывает operation и переключение шага', async() => {
        const {
            result,
        } = renderHook(() => useSteps(
            3,
            stepsRegistry,
            setCurrentStep,
            false,
        ));

        await act(async() => {
            await result.current.goToNextStep(operation);
        });

        expect(runOrder).toHaveBeenNthCalledWith(1, 'operation');
        expect(runOrder).toHaveBeenNthCalledWith(2, 'setCurrentStep');
        expect(setCurrentStep).toHaveBeenCalledWith(3);
    });
});

describe('goToPreviousStep', () => {
    it('если ничего не передано, вызывает переключение шага', async() => {
        const {
            result,
        } = renderHook(() => useSteps(
            1,
            stepsRegistry,
            setCurrentStep,
            false,
        ));

        await act(async() => {
            await result.current.goToPreviousStep();
        });

        expect(runOrder).toHaveBeenNthCalledWith(1, 'setCurrentStep');
        expect(setCurrentStep).toHaveBeenCalledWith(1);
    });

    it('если передан только operation, вызывает operation и переключение шага', async() => {
        const {
            result,
        } = renderHook(() => useSteps(
            2,
            stepsRegistry,
            setCurrentStep,
            false,
        ));

        await act(async() => {
            await result.current.goToPreviousStep(operation);
        });

        expect(runOrder).toHaveBeenNthCalledWith(1, 'operation');
        expect(runOrder).toHaveBeenNthCalledWith(2, 'setCurrentStep');
        expect(setCurrentStep).toHaveBeenCalledWith(1);
    });
});

describe('getStepMetaProps', () => {
    it('первый шаг - текущий', () => {
        const {
            result,
        } = renderHook(() => useSteps(
            1,
            stepsRegistry,
            setCurrentStep,
            false,
        ));

        expect(result.current.getStepMetaProps(1)).toMatchSnapshot();
    });

    it('первый шаг - предыдущий', () => {
        const {
            result,
        } = renderHook(() => useSteps(
            2,
            stepsRegistry,
            setCurrentStep,
            false,
        ));

        expect(result.current.getStepMetaProps(1)).toMatchSnapshot();
    });

    it('первый шаг - до текущего', () => {
        const {
            result,
        } = renderHook(() => useSteps(
            3,
            stepsRegistry,
            setCurrentStep,
            false,
        ));

        expect(result.current.getStepMetaProps(1)).toMatchSnapshot();
    });

    it('последний шаг - текущий', () => {
        const {
            result,
        } = renderHook(() => useSteps(
            3,
            stepsRegistry,
            setCurrentStep,
            false,
        ));

        expect(result.current.getStepMetaProps(3)).toMatchSnapshot();
    });

    it('последний шаг - следующий', () => {
        const {
            result,
        } = renderHook(() => useSteps(
            2,
            stepsRegistry,
            setCurrentStep,
            false,
        ));

        expect(result.current.getStepMetaProps(3)).toMatchSnapshot();
    });

    it('последний шаг - после текущего', () => {
        const {
            result,
        } = renderHook(() => useSteps(
            1,
            stepsRegistry,
            setCurrentStep,
            false,
        ));

        expect(result.current.getStepMetaProps(3)).toMatchSnapshot();
    });

    it('средний шаг - предыдущий', () => {
        const {
            result,
        } = renderHook(() => useSteps(
            3,
            stepsRegistry,
            setCurrentStep,
            false,
        ));

        expect(result.current.getStepMetaProps(2)).toMatchSnapshot();
    });

    it('средний шаг - текущий', () => {
        const {
            result,
        } = renderHook(() => useSteps(
            2,
            stepsRegistry,
            setCurrentStep,
            false,
        ));

        expect(result.current.getStepMetaProps(2)).toMatchSnapshot();
    });

    it('средний шаг - следующий', () => {
        const {
            result,
        } = renderHook(() => useSteps(
            1,
            stepsRegistry,
            setCurrentStep,
            false,
        ));

        expect(result.current.getStepMetaProps(2)).toMatchSnapshot();
    });

    it('без initialStepOrder (выбираем первый)', () => {
        const {
            result,
        } = renderHook(() => useSteps(
            null,
            stepsRegistry,
            setCurrentStep,
            false,
        ));

        expect(result.current.getStepMetaProps(1).isCurrent).toEqual(true);
    });

    it('когда текущий шаг еще не зареган', () => {
        const {
            result,
        } = renderHook(() => useSteps(
            5,
            stepsRegistry,
            setCurrentStep,
            false,
        ));

        expect(result.current.getStepMetaProps(5)).toMatchSnapshot();
    });
});
