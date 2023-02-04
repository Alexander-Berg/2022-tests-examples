import { renderHook } from '@testing-library/react-hooks';

import useDebouncedCallback from './useDebouncedCallback';

describe('useDebouncedCallback', () => {
    it('Возвращает одну и ту же функцию при изменении ссылки на переданый callback', () => {
        const callback = jest.fn();
        const { rerender, result } = renderHook(({ callback }) => useDebouncedCallback(callback, {}, []), {
            initialProps: {
                callback,
            },
        });
        const debouncedFirst = result.current;
        rerender({ callback: jest.fn() });
        const debouncedSecond = result.current;
        expect(debouncedFirst).toBe(debouncedSecond);
    });

    it('Возвращает разные функции при изменении options', () => {
        const callback = jest.fn();
        const { rerender, result } = renderHook(({ callback, wait }) => useDebouncedCallback(
            callback,
            { wait },
            [],
        ), {
            initialProps: {
                wait: 300,
                callback,
            },
        });
        const debouncedFirst = result.current;
        rerender({ wait: 500, callback });
        const debouncedSecond = result.current;

        expect(debouncedFirst).not.toBe(debouncedSecond);
    });

    it('Возвращает разные функции при изменении дополнительного массива зависимостей', () => {
        const callback = jest.fn();
        // eslint-disable-next-line
        const { rerender, result } = renderHook<{
            wait: number;
            callback: (...args: Array<never>) => unknown;
            deps: Array<unknown>;
        }, () => void>(({ callback, deps, wait }) => useDebouncedCallback(
                    callback,
                    { wait },
                    deps,
                ), {
                    initialProps: {
                        wait: 300,
                        callback,
                        deps: [ 'anyDeps' ],
                    },
                });
        let debouncedFirst = result.current;
        rerender({ wait: 300, callback, deps: [ 'otherDeps' ] });
        let debouncedSecond = result.current;

        expect(debouncedFirst).not.toBe(debouncedSecond);

        //разные ссылки у объектов внутри deps
        rerender({ wait: 300, callback, deps: [ { id: 1 } ] });
        debouncedFirst = result.current;
        rerender({ wait: 300, callback, deps: [ { id: 1 } ] });
        debouncedSecond = result.current;

        expect(debouncedFirst).not.toBe(debouncedSecond);
    });

    it('Возвращает одну и ту же функцию если дополнительный массив зависимостей не менялся', () => {
        const callback = jest.fn();
        const deps = [ { id: 1 } ];
        // eslint-disable-next-line
        const { rerender, result } = renderHook<{
            wait: number;
            callback: (...args: Array<never>) => unknown;
            deps: Array<unknown>;
        }, () => void>(({ callback, deps, wait }) => useDebouncedCallback(
                    callback,
                    { wait },
                    deps,
                ), {
                    initialProps: {
                        wait: 300,
                        callback,
                        deps,
                    },
                });
        const debouncedFirst = result.current;
        rerender({ wait: 300, callback, deps });
        const debouncedSecond = result.current;

        expect(debouncedFirst).toBe(debouncedSecond);

    });
});
