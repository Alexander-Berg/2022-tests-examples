/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import { renderHook, act } from '@testing-library/react-hooks';

import sleep from 'auto-core/lib/sleep';

import * as mockUseWizardContext from 'auto-core/react/components/common/Wizard/contexts/wizardContext.mock';

jest.mock('auto-core/react/components/common/Wizard/contexts/WizardContext', () => mockUseWizardContext);

const {
    stepHelpers,
    stepMetaProps,
    useWizardContext,
    useWizardContextMock,
} = mockUseWizardContext;

beforeEach(async() => {
    useWizardContextMock.registerControls.mockClear();
    useWizardContextMock.unregisterControls.mockClear();
    useWizardContextMock.getStepHelpers.mockClear();
    useWizardContextMock.getStepMetaProps.mockClear();
    useWizardContextMock.goToNextStep.mockClear();
    useWizardContext.mockClear();

    useWizardContextMock.currentStepOrder = 1;
});

import useWizardControls from './useWizardControls';

it('вызывает registerContent на первом рендере', () => {
    const {
        rerender,
    } = renderHook(() => useWizardControls({}));

    // регистрация до маунта
    expect(useWizardContextMock.registerControls).toHaveBeenCalledTimes(1);

    act(() => {
        rerender(true);
    });

    // не вызывает на ререндере
    expect(useWizardContextMock.registerControls).toHaveBeenCalledTimes(1);
});

it('вызывает unregisterContent на unmount', () => {
    const {
        rerender,
        unmount,
    } = renderHook(() => useWizardControls({}));

    expect(useWizardContextMock.unregisterControls).toHaveBeenCalledTimes(0);

    act(() => {
        rerender(true);
    });

    expect(useWizardContextMock.unregisterControls).toHaveBeenCalledTimes(0);

    unmount();

    expect(useWizardContextMock.unregisterControls).toHaveBeenCalledTimes(1);
});

it('меняет возвращаемый isProcessingState при обновлении isProcessing', async() => {
    const {
        rerender,
        result,
    } = renderHook(() => useWizardControls({}));

    expect(result.current.isProcessingState).toEqual(false);

    useWizardContextMock.getStepMetaProps.mockImplementationOnce(() => ({
        ...stepMetaProps,
        isProcessing: true,
    }));

    act(() => {
        rerender(true);
    });

    useWizardContextMock.getStepMetaProps.mockImplementationOnce(() => ({
        ...stepMetaProps,
        isProcessing: false,
    }));

    await act(async() => {
        rerender(true);
        // скидывает флаг не сразу
        expect(result.current.isProcessingState).toEqual(true);

        await sleep(300);
    });

    expect(result.current.isProcessingState).toEqual(false);

    useWizardContextMock.getStepMetaProps.mockImplementationOnce(() => ({
        ...stepMetaProps,
        isProcessing: true,
    }));

    act(() => {
        rerender(true);
    });

    useWizardContextMock.getStepMetaProps.mockImplementationOnce(() => ({
        ...stepMetaProps,
        isProcessing: false,
    }));

    act(() => {
        rerender(true);
    });

    expect(result.current.isProcessingState).toEqual(true);

    useWizardContextMock.getStepMetaProps.mockImplementationOnce(() => ({
        ...stepMetaProps,
        isProcessing: true,
    }));

    await act(async() => {
        rerender(true);

        await sleep(300);
    });

    // оставляет true, если сразу поставили обратно true
    expect(result.current.isProcessingState).toEqual(true);
});

describe('handleNextClick', () => {
    it('не вызывает обработчики во время isProcessingState', () => {
        const onFinish = jest.fn();
        const onNextClick = jest.fn();

        useWizardContextMock.getStepMetaProps.mockImplementationOnce(() => ({
            ...stepMetaProps,
            isProcessing: true,
        }));

        const {
            result,
        } = renderHook(() => useWizardControls({
            onNextClick,
            onFinish,
        }));

        const handleNextClick = result.current.handleNextClick;

        act(() => {
            handleNextClick();
        });

        expect(useWizardContextMock.goToNextStep).toHaveBeenCalledTimes(0);
        expect(onFinish).toHaveBeenCalledTimes(0);
        expect(onNextClick).toHaveBeenCalledTimes(0);
    });

    it('вызывает onNextClick с нужными параметрами, если он передан', () => {
        const onFinish = jest.fn();
        const onNextClick = jest.fn();

        useWizardContextMock.getStepMetaProps.mockImplementationOnce(() => ({
            ...stepMetaProps,
            isProcessing: false,
        }));

        const {
            result,
        } = renderHook(() => useWizardControls({
            onNextClick,
            onFinish,
        }));

        const handleNextClick = result.current.handleNextClick;

        act(() => {
            handleNextClick();
        });

        expect(useWizardContextMock.goToNextStep).toHaveBeenCalledTimes(0);
        expect(onFinish).toHaveBeenCalledTimes(0);
        expect(onNextClick).toHaveBeenCalledTimes(1);
        expect(onNextClick).toHaveBeenCalledWith({
            ...stepMetaProps,
            ...stepHelpers,
        });
    });

    it('вызывает goToNextStep, если нет иных callback\'ов', () => {
        const {
            result,
        } = renderHook(() => useWizardControls({}));

        const handleNextClick = result.current.handleNextClick;

        act(() => {
            handleNextClick();
        });

        expect(useWizardContextMock.goToNextStep).toHaveBeenCalledTimes(1);
        expect(useWizardContextMock.goToNextStep).toHaveBeenCalledWith(undefined);
    });

    it('вызывает goToNextStep c передачей onFinish для последнего шага', () => {
        return new Promise((done) => {
            const onFinish = jest.fn((stepProps) => Promise.resolve(stepProps));

            useWizardContextMock.goToNextStep.mockImplementationOnce((operation) => {
                return operation?.().then((result: any) => {
                    expect(result).toEqual({
                        ...stepMetaProps,
                        ...stepHelpers,
                        isLast: true,
                    });

                    done(true);
                });
            });

            useWizardContextMock.getStepMetaProps.mockImplementationOnce(() => ({
                ...stepMetaProps,
                isLast: true,
            }));

            const {
                result,
            } = renderHook(() => useWizardControls({
                onFinish,
            }));

            const handleNextClick = result.current.handleNextClick;

            act(() => {
                handleNextClick();
            });

            expect(useWizardContextMock.goToNextStep).toHaveBeenCalledTimes(1);
        });
    });
});
