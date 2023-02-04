/**
 * @jest-environment node
 */
import { renderHook, act } from '@testing-library/react-hooks';

import { LISTING_BLOCK_QUIZ_FOR_MAP_MOCK } from 'core/services/postListing/mocks/listingBlock.mock';
import { mockStore } from 'core/mocks/store.mock';
import { ROUTER_SERVICE_MOCK_1 } from 'core/services/router/mocks/routerService.mock';

import { useListingBlockQuiz } from './useListingBlockQuiz';

describe('правильно подготавливает данные', () => {
    mockStore({ router: ROUTER_SERVICE_MOCK_1 });

    const { result } = renderHook(() =>
        useListingBlockQuiz(LISTING_BLOCK_QUIZ_FOR_MAP_MOCK),
    );

    it('заголовок', () => {
        expect(result.current.title).toBe('Тесты');
    });

    it('данные для кнопок', () => {
        expect(result.current.buttons).toEqual({
            listingHref: '/journal/tag/research/',
            listingText: 'Все идеи',
            postHref: '/journal/post/kviz-ugadayte-skolko-stoit-arenda-kvartiry-na-foto/',
            postText: 'Пройти тест дальше',
        });
    });

    it('пост', () => {
        expect(result.current.post).toEqual(LISTING_BLOCK_QUIZ_FOR_MAP_MOCK.data.quiz.post);
    });

    it('вопрос', () => {
        expect(result.current.question).toBe(LISTING_BLOCK_QUIZ_FOR_MAP_MOCK.data.quiz.post.quiz.firstQuestion.question);
    });

    it('картинка вопроса', () => {
        expect(result.current.questionImage).toBe(LISTING_BLOCK_QUIZ_FOR_MAP_MOCK.data.quiz.post.quiz.firstQuestion.image);
    });

    it('каунтер', () => {
        expect(result.current.previewCounter).toBe('1/7');
    });

    it('ответы', () => {
        expect(result.current.answers).toEqual([
            { answerIndex: 0, checked: false, title: '<p>28 000 руб.</p>' },
            { answerIndex: 1, checked: false, title: '<p>46 000 руб.</p>' },
            { answerIndex: 2, checked: false, title: '<p>82 000 руб.</p>' },
        ]);
    });
});

it('меняет состояние ответа, если сменить значение', () => {
    const { result  } = renderHook(() =>
        useListingBlockQuiz(LISTING_BLOCK_QUIZ_FOR_MAP_MOCK),
    );

    act(() => {
        result.current.changeValue(1);
    });

    expect(result.current.answers).toEqual([
        { answerIndex: 0, checked: false, title: '<p>28 000 руб.</p>' },
        { answerIndex: 1, checked: true, title: '<p>46 000 руб.</p>' },
        { answerIndex: 2, checked: false, title: '<p>82 000 руб.</p>' },
    ]);
});
