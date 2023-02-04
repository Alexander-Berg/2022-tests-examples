import React from 'react';
import { renderHook } from '@testing-library/react-hooks';

import {
    useWizardStepContext as mockUseWizardStepContext,
    useWizardStepContextMock,
} from 'auto-core/react/components/common/Wizard/contexts/wizardStepContext.mock';

jest.mock('auto-core/react/components/common/Wizard/contexts/WizardStepContext', () => ({
    useWizardStepContext: mockUseWizardStepContext,
}));

let mockScrollY = 0;

jest.mock('auto-core/react/hooks/useScroll', () => {
    return () => ({
        y: mockScrollY,
    });
});

beforeEach(() => {
    useWizardStepContextMock.stepContainerRef = React.createRef();
    useWizardStepContextMock.setStepHeader.mockClear();

    mockScrollY = 0;
});

import useWizardStepHeader from './useWizardStepHeader';

it('устанавливает нужный заголовок', () => {
    const header = 'заголовок';

    renderHook((props) => useWizardStepHeader(props), {
        initialProps: {
            header,
        },
    });

    expect(useWizardStepContextMock.setStepHeader).toHaveBeenCalledTimes(1);
    expect(useWizardStepContextMock.setStepHeader).toHaveBeenCalledWith(header);
});

describe('рассчитывает opacity', () => {
    const header = 'заголовок';

    it('для непроскролленного блока', async() => {
        mockUseWizardStepContext.mockImplementationOnce(() => ({
            ...useWizardStepContextMock,
            isCurrent: true,
        }));

        const {
            rerender,
            result,
        } = renderHook((props) => useWizardStepHeader(props), {
            initialProps: {
                header,
            },
        });

        result.current.headerRef({
            offsetTop: 100,
            clientHeight: 50,
        });

        rerender();

        expect(result.current.opacity).toEqual(1);
    });

    it('всегда 1 для не текущего блока', () => {
        mockScrollY = 200;

        mockUseWizardStepContext.mockImplementationOnce(() => ({
            ...useWizardStepContextMock,
            isCurrent: false,
        }));

        const {
            rerender,
            result,
        } = renderHook((props) => useWizardStepHeader(props), {
            initialProps: {
                header,
            },
        });

        result.current.headerRef({
            offsetTop: 100,
            clientHeight: 50,
        });

        rerender();

        expect(result.current.opacity).toEqual(1);
    });

    it('для проскролленного блока до заголовка', async() => {
        mockScrollY = 100;

        mockUseWizardStepContext.mockImplementationOnce(() => ({
            ...useWizardStepContextMock,
            isCurrent: true,
        }));

        const {
            rerender,
            result,
        } = renderHook((props) => useWizardStepHeader(props), {
            initialProps: {
                header,
            },
        });

        result.current.headerRef({
            offsetTop: 100,
            clientHeight: 50,
        });

        rerender();

        expect(result.current.opacity).toEqual(0.04947163145822209);
    });

    it('для проскролленного блока за заголовок', async() => {
        mockScrollY = 200;

        mockUseWizardStepContext.mockImplementationOnce(() => ({
            ...useWizardStepContextMock,
            isCurrent: true,
        }));

        const {
            rerender,
            result,
        } = renderHook((props) => useWizardStepHeader(props), {
            initialProps: {
                header,
            },
        });

        result.current.headerRef({
            offsetTop: 100,
            clientHeight: 50,
        });

        rerender();

        expect(result.current.opacity).toEqual(0);
    });
});
