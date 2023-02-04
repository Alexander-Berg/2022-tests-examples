/* eslint-disable @typescript-eslint/no-explicit-any */

import { FetchHttpError } from '../../lib/errors';
import errorByStatusDecorator from '../error-by-status';

describe('Decorator ErrorByStatus', () => {
    test('throw error', async () => {
        const baseFetch = jest.fn().mockResolvedValue({ status: 400 });
        const fetch = errorByStatusDecorator(baseFetch);
        const errorByStatus = jest.fn().mockReturnValue(true);

        try {
            await fetch('url', { errorByStatus });
        } catch (err) {
            expect(err).toBeInstanceOf(FetchHttpError);
        }
    });

    test('pass response', async () => {
        const baseFetch = jest.fn().mockResolvedValue({ status: 400 });
        const fetch = errorByStatusDecorator(baseFetch);
        const errorByStatus = jest.fn().mockReturnValue(false);

        const res = await fetch('url', { errorByStatus });
        expect(res).toEqual({ status: 400 });
    });

    test('checkStatus by option', async () => {
        const baseFetch = jest.fn().mockResolvedValue({ status: 201 });
        const errorByStatus = jest.fn().mockReturnValue(false);
        const fetch = errorByStatusDecorator(baseFetch);

        await fetch('url', { errorByStatus });
        expect(errorByStatus).toBeCalledWith(201);
    });

    test('should handle fake Response from tryCatch decorator', async () => {
        const baseFetch = jest.fn().mockResolvedValue({
            status: -1,
            statusText: 'Foobar',
            url: '/url',
            headers: {
                get() {},
            },
        });
        const errorByStatus = jest.fn().mockReturnValue(true);
        const fetch = errorByStatusDecorator(baseFetch);

        await expect(fetch('url', { errorByStatus })).rejects.toThrow('Foobar');
    });
});
