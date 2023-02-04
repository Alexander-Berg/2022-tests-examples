/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import { renderHook, act } from '@testing-library/react-hooks';

import cookie from 'auto-core/react/lib/cookie';

import {
    SUBSCRIPTION_FORM_COOKIE_KEY,
    SUBSCRIPTION_FORM_COOKIE_VALUE,
    SUBSCRIPTION_FORM_COOKIE_EXPIRES,
} from '../constants';

import useForm from './useForm';

const VALUE_MOCK = 'dog@mag.ru';

jest.mock('auto-core/react/lib/cookie');

it('по умолчанию установлены правильные значения', () => {
    const {
        result: {
            current: {
                isSuccess,
                value,
            },
        },
    } = renderHook(() => useForm());

    expect(value).toBe('');
    expect(isSuccess).toBe(false);
});

it('не вызывается обработчик подписки, не устанавливается значение в хранилище, isSuccess = false, если пустое значение', () => {
    const { result } = renderHook(() => useForm());

    const mindboxMock = jest.fn();

    window.mindbox = mindboxMock;

    act(() => {
        result.current.onSubmit();
    });

    expect(result.current.isSuccess).toBe(false);
    expect(cookie.set).not.toHaveBeenLastCalledWith(
        SUBSCRIPTION_FORM_COOKIE_KEY,
        SUBSCRIPTION_FORM_COOKIE_VALUE,
        { expires: SUBSCRIPTION_FORM_COOKIE_EXPIRES },
    );
    expect(mindboxMock).not.toHaveBeenCalled();
});

it('вызывается обработчик подписки, при заполнения значения и вызове onSubmit', () => {
    const { result } = renderHook(() => useForm());

    const mindboxMock = jest.fn();

    window.mindbox = mindboxMock;

    act(() => {
        result.current.onChangeValue({ value: VALUE_MOCK });
    });
    act(() => {
        result.current.onSubmit();
    });

    expect(result.current.isSuccess).toBe(true);
    expect(cookie.set).toHaveBeenLastCalledWith(
        SUBSCRIPTION_FORM_COOKIE_KEY,
        SUBSCRIPTION_FORM_COOKIE_VALUE,
        { expires: SUBSCRIPTION_FORM_COOKIE_EXPIRES },
    );
    expect(mindboxMock.mock.calls[0]).toMatchSnapshot();
});
