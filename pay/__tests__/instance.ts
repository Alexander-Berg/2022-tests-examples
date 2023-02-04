/* eslint-disable @typescript-eslint/no-explicit-any,@typescript-eslint/ban-ts-ignore */

import createInstance from '../instance';

const decorator =
    (mixin1 = {}, mixin2 = {}) =>
    (fn: any) =>
    (url: any, options: any) => ({
        ...mixin2,
        ...fn(url, { ...mixin1, ...options }),
    });

describe('CreateInstance', () => {
    test('merge options', () => {
        const baseFetch = jest.fn();
        const fetch = createInstance(
            {
                method: 'GET',
                headers: { 'x-debug': 'true' },
                retry: { limit: 1 },
            },
            [],
            baseFetch,
        );

        fetch('url', { credentials: 'include', headers: { 'content-type': '*' } });
        expect(baseFetch).toBeCalledWith('url', {
            method: 'GET',
            headers: { 'content-type': '*' },
            credentials: 'include',
            retry: { limit: 1 },
        });
    });

    test('apply decorators', () => {
        const baseFetch = jest.fn().mockReturnValue({ text: 'resp' });
        const fetch = createInstance(
            { method: 'GET' },
            [decorator({ reqD1: 'd1' }, { resD1: 'd1' }), decorator({ reqD2: 'd2' }, { resD2: 'd2' })],
            baseFetch,
        );

        const response = fetch('url', {});

        expect(baseFetch).toBeCalledWith('url', {
            method: 'GET',
            reqD1: 'd1',
            reqD2: 'd2',
        });

        expect(response).toEqual({ text: 'resp', resD1: 'd1', resD2: 'd2' });
    });
});
