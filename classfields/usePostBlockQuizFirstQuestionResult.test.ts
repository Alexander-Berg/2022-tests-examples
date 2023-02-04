import { renderHook, act } from '@testing-library/react-hooks';

import { SessionStorageKey } from 'core/services/sessionStorage/constants/key';
import { sessionStorage } from 'core/services/sessionStorage/utils/sessionstorage';

import { usePostBlockQuizFirstQuestionResult } from './usePostBlockQuizFirstQuestionResult';

const URL_PART = 'test-post';

beforeEach(() => {
    sessionStorage.clear();
});

it('не находит значение, если его нет в хранилище', () => {
    const { result } = renderHook(() =>
        usePostBlockQuizFirstQuestionResult({
            postUrlPart: URL_PART,
        }),
    );

    expect(result.current.value).toBe(null);
});

it('возвращает значение из хранилище, если он определено', () => {
    const storage = { [URL_PART]: 1 };

    sessionStorage.setItem(SessionStorageKey.LISTING_BLOCK_QUIZ_FIRST_QUESTION_RESULT, JSON.stringify(storage));

    const { result } = renderHook(() =>
        usePostBlockQuizFirstQuestionResult({
            postUrlPart: URL_PART,
        }),
    );

    expect(result.current.value).toBe(1);
});

describe('смена значения', () => {
    it('возвращаемое значение', () => {
        const { result } = renderHook(() =>
            usePostBlockQuizFirstQuestionResult({
                postUrlPart: URL_PART,
            }),
        );

        act(() => {
            result.current.changeValue(2);
        });

        expect(result.current.value).toBe(2);
    });

    it('sessionStorage', () => {
        const { result } = renderHook(() =>
            usePostBlockQuizFirstQuestionResult({
                postUrlPart: URL_PART,
            }),
        );

        act(() => {
            result.current.changeValue(2);
        });

        const value = sessionStorage.getItem(SessionStorageKey.LISTING_BLOCK_QUIZ_FIRST_QUESTION_RESULT);

        expect(value && JSON.parse(value)).toEqual({
            [URL_PART]: 2,
        });
    });
});
