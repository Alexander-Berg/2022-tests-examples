import { renderHook, act } from '@testing-library/react-hooks';

import sleep from 'auto-core/lib/sleep';

import useCreditFormFieldValues from './useCreditFormFieldValues';
import type { UseCreditFormFieldValues } from './useCreditFormFieldValues';

let startForceUpdate: UseCreditFormFieldValues<unknown>['startForceUpdate'];
let valueProcessor: UseCreditFormFieldValues<unknown>['valueProcessor'];
let setFormikValue: UseCreditFormFieldValues<unknown>['setFormikValue'];
let setError: UseCreditFormFieldValues<unknown>['setError'];
let onChange: UseCreditFormFieldValues<unknown>['onChange'];
let onFieldChange: UseCreditFormFieldValues<unknown>['onFieldChange'];

const name = 'field name';

beforeEach(() => {
    startForceUpdate = jest.fn();
    valueProcessor = jest.fn((value) => value + ' processed');
    setFormikValue = jest.fn();
    setError = jest.fn();
    onChange = jest.fn();
    onFieldChange = jest.fn();
});

it('вызывает forceUpdate на изменении значения/ошибки, маунте/анмаунте', async() => {
    const props: UseCreditFormFieldValues<unknown> = {
        name,
        startForceUpdate,
        value: 'field value',
        valueProcessor: undefined,
        setFormikValue,
        setError,
        onChange,
        onFieldChange,
    };

    const {
        rerender,
    } = renderHook((props) => useCreditFormFieldValues(props), {
        initialProps: props,
    });

    expect(startForceUpdate).toHaveBeenCalledTimes(1);

    await act(async() => {
        rerender({
            ...props,
            value: 'field value 2',
        });

        await sleep(0);

        expect(startForceUpdate).toHaveBeenCalledTimes(2);

        rerender({
            ...props,
            value: 'field value 2',
            error: 'error',
        });

        await sleep(0);

        expect(startForceUpdate).toHaveBeenCalledTimes(3);
    });
});

it('при изменении значения поля вызывает setFormikValue с тем же значением, если valueProcessor не передан', () => {
    const props: UseCreditFormFieldValues<unknown> = {
        name,
        startForceUpdate,
        value: 'field value',
        valueProcessor: undefined,
        setFormikValue,
        setError,
        onChange,
        onFieldChange,
    };

    const {
        result,
    } = renderHook((props) => useCreditFormFieldValues(props), {
        initialProps: props,
    });

    result.current.setValue('value 2');

    expect(setFormikValue).toHaveBeenCalledWith('value 2');
});

it('при изменении значения поля вызывает setFormikValue с подготовленным значением, если передан valueProcessor', () => {
    const props: UseCreditFormFieldValues<unknown> = {
        name,
        startForceUpdate,
        value: 'field value',
        valueProcessor,
        setFormikValue,
        setError,
        onChange,
        onFieldChange,
    };

    const {
        result,
    } = renderHook((props) => useCreditFormFieldValues(props), {
        initialProps: props,
    });

    result.current.setValue('value 2');

    expect(setFormikValue).toHaveBeenCalledWith('value 2 processed');
});

it('при изменении значения поля скидывает ошибку', () => {
    const props: UseCreditFormFieldValues<unknown> = {
        name,
        startForceUpdate,
        value: 'field value',
        valueProcessor: undefined,
        setFormikValue,
        setError,
        onChange,
        onFieldChange,
    };

    const {
        result,
    } = renderHook((props) => useCreditFormFieldValues(props), {
        initialProps: props,
    });

    result.current.setValue('value 2');

    expect(setError).toHaveBeenCalledWith(undefined);
});

it('при изменении значения поля вызывает onFieldChange, если он передан', () => {
    const props: UseCreditFormFieldValues<unknown> = {
        name,
        startForceUpdate,
        value: 'field value',
        valueProcessor: undefined,
        setFormikValue,
        setError,
        onChange,
        onFieldChange,
    };

    const {
        result,
    } = renderHook((props) => useCreditFormFieldValues(props), {
        initialProps: props,
    });

    result.current.setValue('value 2');

    expect(onFieldChange).toHaveBeenCalledWith(name, 'value 2');
});

it('handleChange вызывает изменение значения поля', () => {
    const props: UseCreditFormFieldValues<unknown> = {
        name,
        startForceUpdate,
        value: 'field value',
        valueProcessor: undefined,
        setFormikValue,
        setError,
        onChange,
        onFieldChange,
    };

    const {
        result,
    } = renderHook((props) => useCreditFormFieldValues(props), {
        initialProps: props,
    });

    const event = { target: { value: 'new value' } };

    result.current.handleChange(event);

    expect(setFormikValue).toHaveBeenCalledWith(event.target.value);
});

it('handleChange вызывает onChange', () => {
    const props: UseCreditFormFieldValues<unknown> = {
        name,
        startForceUpdate,
        value: 'field value',
        valueProcessor: undefined,
        setFormikValue,
        setError,
        onChange,
        onFieldChange,
    };

    const {
        result,
    } = renderHook((props) => useCreditFormFieldValues(props), {
        initialProps: props,
    });

    const event = { target: { value: 'new value' } };

    result.current.handleChange(event);

    expect(onChange).toHaveBeenCalledWith(event);
});
