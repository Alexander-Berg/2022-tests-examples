import { renderHook, act } from '@testing-library/react-hooks';

import gateApi from 'auto-core/react/lib/gateApi';

import { useContentMark } from './useContentMark';

jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(() => Promise.resolve()),
    };
});

const getResource = gateApi.getResource as jest.MockedFunction<typeof gateApi.getResource>;

const USER_ID = '123';

describe('useContentMark', () => {
    beforeAll(() => {
        jest.useFakeTimers();
    });

    afterAll(() => {
        jest.useRealTimers();
    });

    it('выполнит запрос, что новости были просмотрен, после добавления нескольких id подряд', async() => {
        const { result, waitFor } = renderHook(() => useContentMark({ userId: USER_ID }));
        act(() => {
            result.current.setContentIds((state) => [ ...state, '1' ]);
        });

        expect(result.current.ids).toEqual([ '1' ]);

        act(() => {
            result.current.setContentIds((state) => [ ...state, '2' ]);
        });

        expect(result.current.ids).toEqual([ '1', '2' ]);

        act(() => {
            result.current.setContentIds((state) => [ ...state, '3' ]);
        });

        expect(result.current.ids).toEqual([ '1', '2', '3' ]);

        act(() => {
            jest.runAllTimers();
        });

        await waitFor(() => {
            expect(result.current.ids).toEqual([]);
        });

        expect(getResource).toHaveBeenCalledTimes(1);
        expect(getResource).toHaveBeenCalledWith('markRead', {
            user_id: `user:${ USER_ID }`,
            payload: {
                content_read_state: [
                    {
                        content_id: '1',
                        was_seen_preview: true,
                    },
                    {
                        content_id: '2',
                        was_seen_preview: true,
                    },
                    {
                        content_id: '3',
                        was_seen_preview: true,
                    },
                ],
            },
        });
    });

    it('не выполнит запрос, что новости были просмотрен, так как не был передан userId', async() => {
        const { result } = renderHook(() => useContentMark({ userId: undefined }));
        act(() => {
            result.current.setContentIds((state) => [ ...state, '1' ]);
        });

        expect(result.current.ids).toEqual([ '1' ]);

        act(() => {
            jest.runAllTimers();
        });

        expect(getResource).toHaveBeenCalledTimes(0);
    });
});
