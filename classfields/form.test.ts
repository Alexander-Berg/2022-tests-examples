import {
    getErrors,
    getMessage,
    isDateFormat,
    isWithoutErrors,
    notEmpty,
    runCheck,
    setValue,
    simpleSetter,
    translateErrors,
} from './form';
import type { TFieldList } from './form';

enum Field {
    TEST = 'TEST',
}

interface TObj {
    test: string;
}

type TValues = TObj

const fields: TFieldList<TObj, TValues> = [
    { name: 'test', valid: ({ test }): string => test + test, setter: simpleSetter },
];

it('getErrors возвращает ошибки, если есть', () => {
    const errors = getErrors<TObj, TValues>(fields, { test: 'error' });

    expect(errors.test).toBe('errorerror');

    const errors2 = getErrors<TObj, TValues>(fields, { test: '' });

    expect(isWithoutErrors(errors2)).toBe(true);
});

it('getMessage возвращает сообщения, если есть', () => {
    const message = getMessage<TObj>({ errors: { test: 'message' }, keys: 'test' });

    expect(message).toBe('message');

    const message2 = getMessage<TObj>({ errors: { }, keys: 'test' });

    expect(message2).toBeNull();
});

it('isDateFormat возвращает ошибку если есть', () => {
    const errorIsAbsent = isDateFormat<TValues>('test', 'DD.MM.YYYY', 'error')({ test: '25.09.1981' });

    expect(errorIsAbsent).toEqual(false);

    const error = isDateFormat<TValues>('test', 'DD.MM.YYYY', 'error')({ test: '25.9.1981' });

    expect(error).toBe('error');
});

it('notEmpty возвращает ошибку если есть на то причина', () => {
    const errorIsAbsent = notEmpty<TValues>('test', 'error')({ test: '25.09.1981' });

    expect(errorIsAbsent).toEqual(false);

    const error = notEmpty<TValues>('test', 'error')({ test: '' });

    expect(error).toBe('error');
});

it('runCheck возвращает ошибку при проверке', () => {
    const errorIsAbsent = runCheck<TObj>(() => false, { test: 'test' });

    expect(errorIsAbsent).toEqual(false);

    const error = runCheck<TObj>(() => 'error', { test: 'test' });

    expect(error).toBe('error');
});

it('setValue устанавливает значение', () => {
    const obj = setValue<TObj, TValues>(fields, 'test', '!!!', { test: 'test' });

    expect(obj.test).toBe('!!!');
});

it('translateErrors превращает ошибки (серверные) в пользовательские', () => {
    const obj = translateErrors<Field, TValues>(
        { validation_results: [ { field: Field.TEST, description: 'fuck!' } ], error: 'VALIDATION_ERROR' },
        [ { field: Field.TEST, error: 'test' } ],
    );

    expect(obj.test).toBe('fuck!');
});
