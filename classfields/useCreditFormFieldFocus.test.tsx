/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import { renderHook, act } from '@testing-library/react-hooks';

import sleep from 'auto-core/lib/sleep';

import {
    HighlightModes,
} from 'auto-core/react/components/common/CreditForm/types';

import useCreditFormFieldFocus from './useCreditFormFieldFocus';
import type { UseCreditFormFieldFocus } from './useCreditFormFieldFocus';

const name = 'field name';

let onBlur: UseCreditFormFieldFocus<unknown>['onBlur'];
let handleFieldFocus: UseCreditFormFieldFocus<unknown>['handleFieldFocus'];
let validateField: UseCreditFormFieldFocus<unknown>['validateField'];
let fieldRef: UseCreditFormFieldFocus<unknown>['fieldRef'];

beforeEach(() => {
    onBlur = jest.fn();
    handleFieldFocus = jest.fn();
    validateField = jest.fn();
    fieldRef = {
        current: document.createElement('div'),
    };
});

import './useCreditFormFieldFocus.test.css';

it('handleBlur снимает фокус, и вызывает валидацию по таймауту', async() => {
    const props: UseCreditFormFieldFocus<unknown> = {
        name,
        onBlur,
        handleFieldFocus,
        validateField,
        fieldRef,
    };

    const {
        result,
    } = renderHook((props) => useCreditFormFieldFocus(props), {
        initialProps: props,
    });

    act(() => {
        // поставим фокус, чтоб проверить, что он дальше скинется
        result.current.handleFocus();
    });

    expect(result.current.isFocused).toEqual(true);

    act(() => {
        result.current.handleBlur();
    });

    expect(result.current.isFocused).toEqual(false);
    expect(onBlur).toHaveBeenCalledTimes(1);
    expect(onBlur).toHaveBeenCalledWith(name);

    expect(validateField).toHaveBeenCalledTimes(0);

    await sleep(50);

    expect(validateField).toHaveBeenCalledTimes(1);
    expect(validateField).toHaveBeenCalledWith(name);
    expect(handleFieldFocus).toHaveBeenCalledWith(name, false);
});

it('handleFocus ставит фокус и скидывает валидацию на blur', async() => {
    const props: UseCreditFormFieldFocus<unknown> = {
        name,
        onBlur,
        handleFieldFocus,
        validateField,
        fieldRef,
    };

    const {
        result,
    } = renderHook((props) => useCreditFormFieldFocus(props), {
        initialProps: props,
    });

    act(() => {
        result.current.handleBlur();
    });

    act(() => {
        result.current.handleFocus();
    });

    expect(result.current.isFocused).toEqual(true);
    expect(handleFieldFocus).toHaveBeenCalledTimes(1);
    expect(handleFieldFocus).toHaveBeenCalledWith(name, true);

    await sleep(50);

    expect(validateField).toHaveBeenCalledTimes(0);
});

it('handleFocusChange вызывает смену фокуса', () => {
    const props: UseCreditFormFieldFocus<unknown> = {
        name,
        onBlur,
        handleFieldFocus,
        validateField,
        fieldRef,
    };

    const {
        result,
    } = renderHook((props) => useCreditFormFieldFocus(props), {
        initialProps: props,
    });

    expect(result.current.isFocused).toEqual(false);

    act(() => {
        result.current.handleFocusChange(true);
    });

    expect(result.current.isFocused).toEqual(true);

    act(() => {
        result.current.handleFocusChange(false);
    });

    expect(result.current.isFocused).toEqual(false);
});

it('focus ставит фокус, blur снимает, getFocused возвращает состояние фокуса', () => {
    const props: UseCreditFormFieldFocus<unknown> = {
        name,
        onBlur,
        handleFieldFocus,
        validateField,
        fieldRef,
    };

    const {
        result,
    } = renderHook((props) => useCreditFormFieldFocus(props), {
        initialProps: props,
    });

    expect(result.current.isFocused).toEqual(false);
    expect(result.current.getFocused()).toEqual(false);

    act(() => {
        result.current.focus();
    });

    expect(result.current.isFocused).toEqual(true);
    expect(result.current.getFocused()).toEqual(true);

    act(() => {
        result.current.blur();
    });

    expect(result.current.isFocused).toEqual(false);
    expect(result.current.getFocused()).toEqual(false);
});

it('highlight меняет highlightMode по таймауту и скидывает таймаут unhighlight', async() => {
    const props: UseCreditFormFieldFocus<unknown> = {
        name,
        onBlur,
        handleFieldFocus,
        validateField,
        fieldRef,
        highlightFocusedField: true,
    };

    const {
        result,
    } = renderHook((props) => useCreditFormFieldFocus(props), {
        initialProps: props,
    });

    expect(result.current.highlightMode).toEqual(HighlightModes.HIGHLIGHTED);

    await act(async() => {
        result.current.unhighlight(true);
        result.current.highlight();
        await sleep(70);
    });

    expect(result.current.highlightMode).toEqual(HighlightModes.HIGHLIGHTED);
});

it('highlight НЕ меняет highlightMode, если не передан highlightFocusedField', async() => {
    const props: UseCreditFormFieldFocus<unknown> = {
        name,
        onBlur,
        handleFieldFocus,
        validateField,
        fieldRef,
    };

    const {
        result,
    } = renderHook((props) => useCreditFormFieldFocus(props), {
        initialProps: props,
    });

    expect(result.current.highlightMode).toEqual(HighlightModes.HIGHLIGHTED);

    await act(async() => {
        result.current.highlight();

        await sleep(70);
    });

    expect(result.current.highlightMode).toEqual(HighlightModes.HIGHLIGHTED);
});

it('unhighlight меняет highlightMode по таймауту и скидывает таймаут highlight', async() => {
    const props: UseCreditFormFieldFocus<unknown> = {
        name,
        onBlur,
        handleFieldFocus,
        validateField,
        fieldRef,
        highlightFocusedField: true,
    };

    const {
        result,
    } = renderHook((props) => useCreditFormFieldFocus(props), {
        initialProps: props,
    });

    expect(result.current.highlightMode).toEqual(HighlightModes.HIGHLIGHTED);

    await act(async() => {
        result.current.highlight();
        result.current.unhighlight(true);

        await sleep(70);
    });

    expect(result.current.highlightMode).toEqual(HighlightModes.HIDDEN);
});

it('unhighlight НЕ меняет highlightMode, если не передан highlightFocusedField', async() => {
    const props: UseCreditFormFieldFocus<unknown> = {
        name,
        onBlur,
        handleFieldFocus,
        validateField,
        fieldRef,
    };

    const {
        result,
    } = renderHook((props) => useCreditFormFieldFocus(props), {
        initialProps: props,
    });

    expect(result.current.highlightMode).toEqual(HighlightModes.HIGHLIGHTED);

    await act(async() => {
        result.current.unhighlight(false);

        await sleep(70);
    });

    expect(result.current.highlightMode).toEqual(HighlightModes.HIGHLIGHTED);
});

/*
    TODO: выставляет правильное смещение при фокусе и скроллит вверх (хочется это в браузерном проверить, которые пока не понятно как делать
 */
