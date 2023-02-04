import { renderHook, act } from '@testing-library/react-hooks';

import * as mockUseWizardContext from 'auto-core/react/components/common/Wizard/contexts/wizardContext.mock';

jest.mock('auto-core/react/components/common/Wizard/contexts/WizardContext', () => mockUseWizardContext);

const {
    useWizardContext,
    useWizardContextMock,
} = mockUseWizardContext;

beforeEach(() => {
    useWizardContextMock.registerStep.mockClear();
    useWizardContextMock.unregisterStep.mockClear();
    useWizardContextMock.getStepHelpers.mockClear();
    useWizardContextMock.getStepMetaProps.mockClear();
    useWizardContextMock.goToNextStep.mockClear();
    useWizardContext.mockClear();

    useWizardContextMock.currentStepOrder = 1;
});

import useWizardStep from './useWizardStep';

it('вызывает registerStep на первом рендере', async() => {
    const {
        rerender,
        result,
    } = renderHook((props) => useWizardStep(props), {
        initialProps: {
            order: 0,
            maxPath: 0,
        },
    });

    // регистрация до маунта
    expect(useWizardContextMock.registerStep).toHaveBeenCalledTimes(2);
    expect(useWizardContextMock.registerStep).toHaveBeenCalledWith({
        stepContainerRef: result.current.stepContainerRef,
        order: 0,
        canBeSkipped: false,
        selfControlled: false,
        maxPath: 0,
    });

    act(() => {
        rerender({
            order: 5,
            maxPath: 0,
        });
    });

    expect(useWizardContextMock.registerStep).toHaveBeenCalledTimes(3);
    expect(useWizardContextMock.registerStep).toHaveBeenCalledWith({
        stepContainerRef: result.current.stepContainerRef,
        order: 5,
        canBeSkipped: false,
        selfControlled: false,
        maxPath: 0,
    });
});

it('вызывает unregisterStep на unmount и изменении order', () => {
    const {
        rerender,
        unmount,
    } = renderHook((props) => useWizardStep(props), {
        initialProps: {
            order: 0,
            maxPath: 0,
        },
    });

    expect(useWizardContextMock.unregisterStep).toHaveBeenCalledTimes(0);

    act(() => {
        rerender({
            order: 5,
            maxPath: 0,
        });
    });

    expect(useWizardContextMock.unregisterStep).toHaveBeenCalledTimes(1);

    unmount();

    expect(useWizardContextMock.unregisterStep).toHaveBeenCalledTimes(2);
});
