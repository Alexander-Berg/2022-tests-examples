/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import { renderHook, act } from '@testing-library/react-hooks';

import sleep from 'auto-core/lib/sleep';

import * as mockUseWizardContext from 'auto-core/react/components/common/Wizard/contexts/wizardContext.mock';
import useWizardHeader from 'auto-core/react/components/common/Wizard/hooks/useWizardHeader';

jest.mock('auto-core/react/components/common/Wizard/contexts/WizardContext', () => mockUseWizardContext);

const {
    stepMetaProps,
    useWizardContext,
    useWizardContextMock,
} = mockUseWizardContext;

let mockScrollY = 0;

jest.mock('auto-core/react/hooks/useScroll', () => {
    return () => ({
        y: mockScrollY,
    });
});

beforeEach(async() => {
    useWizardContextMock.getStepMetaProps.mockClear();
    useWizardContextMock.goToPreviousStep.mockClear();
    useWizardContext.mockClear();

    useWizardContextMock.currentStepOrder = 1;

    mockScrollY = 0;
});

it('если передан closable, вызывает handleClose', () => {
    const handleClose = jest.fn();

    useWizardContext.mockImplementationOnce(() => ({
        ...useWizardContextMock,
        closable: true,
        // eslint-disable-next-line @typescript-eslint/ban-ts-comment
        // @ts-ignore
        handleClose,
    }));

    const {
        result,
    } = renderHook(() => useWizardHeader({}));

    act(() => {
        result.current.closeHandler();
    });

    expect(handleClose).toHaveBeenCalledTimes(1);
});

it('если передан closable=false, НЕ вызывает handleClose', () => {
    const handleClose = jest.fn();

    useWizardContext.mockImplementationOnce(() => ({
        ...useWizardContextMock,
        closable: false,
        // eslint-disable-next-line @typescript-eslint/ban-ts-comment
        // @ts-ignore
        handleClose,
    }));

    const {
        result,
    } = renderHook(() => useWizardHeader({}));

    act(() => {
        result.current.closeHandler();
    });

    expect(handleClose).toHaveBeenCalledTimes(0);
});

it('prevStepClickHandler вызывает goToPreviousStep', () => {
    const {
        result,
    } = renderHook(() => useWizardHeader({}));

    act(() => {
        result.current.prevStepClickHandler();
    });

    expect(useWizardContextMock.goToPreviousStep).toHaveBeenCalledTimes(1);
});

describe('стили для анимация', () => {
    const header = 'заголовок';

    it('при непроскролленном блоке', () => {
        useWizardContextMock.getStepMetaProps.mockImplementationOnce(() => ({
            ...stepMetaProps,
            header,
        }));

        const {
            result,
        } = renderHook((props: any) => useWizardHeader(props), {

        });

        expect(result.current.headerStyles).toEqual({
            opacity: 0,
            marginTop: '-20px',
        });

        expect(result.current.shadowStyles).toEqual({
            opacity: 0,
        });
    });

    it('при проскролленном блоке', () => {
        mockScrollY = 25;

        useWizardContextMock.getStepMetaProps.mockImplementationOnce(() => ({
            ...stepMetaProps,
            header,
        }));

        const {
            result,
        } = renderHook((props: any) => useWizardHeader(props), {

        });

        expect(result.current.headerStyles).toEqual({
            opacity: 0.43649402392752995,
            marginTop: '-11.270119521449402px',
        });

        expect(result.current.shadowStyles).toEqual({
            opacity: 0.43649402392752995,
        });
    });

    it('при проскролленном далеко блоке', () => {
        mockScrollY = 125;

        useWizardContextMock.getStepMetaProps.mockImplementationOnce(() => ({
            ...stepMetaProps,
            header,
        }));

        const {
            result,
        } = renderHook((props: any) => useWizardHeader(props), {

        });

        expect(result.current.headerStyles).toEqual({
            opacity: 1,
            marginTop: '0px',
        });

        expect(result.current.shadowStyles).toEqual({
            opacity: 1,
        });
    });
});

describe('смена заголовка', () => {
    const header = 'заголовок';
    const newHeader = 'новый заголовок';

    it('если предыдущий заголовок был показан, меняет заголовок отложено', async() => {
        mockScrollY = 125;

        useWizardContextMock.getStepMetaProps.mockImplementationOnce(() => ({
            ...stepMetaProps,
            header,
        }));

        const {
            rerender,
            result,
        } = renderHook((props: any) => useWizardHeader(props), {});

        expect(result.current.currentHeader).toEqual(header);

        await act(async() => {
            mockScrollY = 0;

            rerender({
                customHeader: newHeader,
            });

            await sleep(50);

            expect(result.current.currentHeader).toEqual(header);

            await sleep(400);

            expect(result.current.currentHeader).toEqual(newHeader);
        });
    });

    it('если заголовки показаны одинаково, меняет заголовок отложено', async() => {
        mockScrollY = 125;

        useWizardContextMock.getStepMetaProps.mockImplementationOnce(() => ({
            ...stepMetaProps,
            header,
        }));

        const {
            rerender,
            result,
        } = renderHook((props: any) => useWizardHeader(props), {});

        expect(result.current.currentHeader).toEqual(header);

        await act(async() => {
            mockScrollY = 125;

            rerender({
                customHeader: newHeader,
            });

            await sleep(50);

            expect(result.current.currentHeader).toEqual(header);

            await sleep(400);

            expect(result.current.currentHeader).toEqual(newHeader);
        });
    });

    it('если заголовки показаны НЕ одинаково, меняет заголовок сразу', async() => {
        mockScrollY = 25;

        useWizardContextMock.getStepMetaProps.mockImplementationOnce(() => ({
            ...stepMetaProps,
            header,
        }));

        const {
            rerender,
            result,
        } = renderHook((props: any) => useWizardHeader(props), {});

        expect(result.current.currentHeader).toEqual(header);

        await act(async() => {
            mockScrollY = 125;

            rerender({
                customHeader: newHeader,
            });

            await sleep(50);

            expect(result.current.currentHeader).toEqual(newHeader);
        });
    });
});
