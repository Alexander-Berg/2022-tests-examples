import { renderHook } from '@testing-library/react-hooks';

import type { UseStepRegistryReturnType } from 'auto-core/react/components/common/Wizard/hooks/useStepRegistry';

import useProgress, { InvalidRegistryError } from './useProgress';

describe('useProgress', () => {
    describe('useProgress для крайних случаев', () => {
        it('возвращает 100 если массив шагов пустой', () => {
            const {
                result,
            } = renderHook(() => useProgress(1, []));

            expect(result.current()).toEqual(100);
        });

        it('выбрасывает ошибку если массив шагов почему то не найден текущий элемент', async() => {
            const stepsRegistry = [
                {
                    order: 0,
                    maxPath: 3,
                },
                {
                    order: 1,
                    maxPath: 2,
                },
            ] as UseStepRegistryReturnType['stepsRegistry'];

            try {
                const { result } = renderHook(() => useProgress(2, stepsRegistry));
                result.current();

                throw new Error('Unreachable');
            } catch (e) {
                // eslint-disable-next-line jest/no-conditional-expect
                expect(e instanceof InvalidRegistryError).toBeTruthy();
            }
        });

        it('выбрасывает ошибку если массив шагов почему то не найден первый элемент', async() => {
            const stepsRegistry = [
                {
                    order: 1,
                    maxPath: 3,
                },
                {
                    order: 2,
                    maxPath: 2,
                },
            ] as UseStepRegistryReturnType['stepsRegistry'];

            let err;
            try {
                const { result } = renderHook(() => useProgress(2, stepsRegistry));

                result.current();
            } catch (e) {
                err = e;
            }

            expect(err instanceof InvalidRegistryError).toBe(true);
        });
    });

    describe('useProgress для четного кол-ва шагов', () => {
        const stepsRegistry = [
            {
                order: 0,
                maxPath: 3,
            },
            {
                order: 1,
                maxPath: 2,
            },
            {
                order: 2,
                maxPath: 1,
            },
            {
                order: 3,
                maxPath: 0,
            },
        ] as UseStepRegistryReturnType['stepsRegistry'];

        it('возвращает 0, для первого currentStepOrder', () => {
            const {
                result,
            } = renderHook(() => useProgress(0, stepsRegistry));

            expect(result.current()).toEqual(0);
        });

        it('возвращает 34, для второго currentStepOrder', () => {
            const {
                result,
            } = renderHook(() => useProgress(1, stepsRegistry));

            expect(result.current()).toEqual(34);
        });

        it('возвращает 67, для третьего currentStepOrder', () => {
            const {
                result,
            } = renderHook(() => useProgress(2, stepsRegistry));

            expect(result.current()).toEqual(67);
        });

        it('возвращает 100, для четвертого currentStepOrder', () => {
            const {
                result,
            } = renderHook(() => useProgress(3, stepsRegistry));

            expect(result.current()).toEqual(100);
        });
    });

    describe('useProgress для нечетного кол-ва шагов', () => {
        const stepsRegistry = [
            {
                order: 0,
                maxPath: 2,
            },
            {
                order: 1,
                maxPath: 1,
            },
            {
                order: 2,
                maxPath: 0,
            },
        ] as UseStepRegistryReturnType['stepsRegistry'];

        it('возвращает 0, для первого currentStepOrder', () => {
            const {
                result,
            } = renderHook(() => useProgress(0, stepsRegistry));

            expect(result.current()).toEqual(0);
        });

        it('возвращает 50, для второго currentStepOrder', () => {
            const {
                result,
            } = renderHook(() => useProgress(1, stepsRegistry));

            expect(result.current()).toEqual(50);
        });

        it('возвращает 100, для третьего currentStepOrder', () => {
            const {
                result,
            } = renderHook(() => useProgress(2, stepsRegistry));

            expect(result.current()).toEqual(100);
        });
    });
});
