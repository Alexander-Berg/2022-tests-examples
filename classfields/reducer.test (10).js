const { PAGE_LOADING_SUCCESS } = require('../../actionTypes');
const {
    REVIEW_PUBLISH_RESOLVED,
    REVIEW_REMOVE_RESOLVED,
} = require('./actionTypes');

const reducer = require('./reducer');

let state;
beforeEach(() => {
    state = reducer(undefined, {});
});

describe('PAGE_LOADING_SUCCESS', () => {
    it('должен обработать, если есть myReviews', () => {
        const nextState = reducer(state, {
            type: PAGE_LOADING_SUCCESS,
            payload: {
                myReviews: {
                    reviews: [ 'data' ],
                    pagination: { page: 1 },
                },
            },
        });

        expect(nextState).toEqual({
            data: {
                reviews: [ 'data' ],
                pagination: { page: 1 },
            },
        });
    });

    it('не должен обработать, если нет myReviews', () => {
        const nextState = reducer(state, {
            type: PAGE_LOADING_SUCCESS,
            payload: {},
        });

        expect(nextState).toEqual(state);
        expect(nextState === state).toBe(true);
    });
});

describe('REVIEW_PUBLISH_RESOLVED', () => {
    it('не должен ничего делать, если отзыв не найден', () => {
        const nextState1 = reducer(state, {
            type: PAGE_LOADING_SUCCESS,
            payload: {
                myReviews: {
                    reviews: [ { id: 1 } ],
                    pagination: { page: 1 },
                },
            },
        });

        const nextState2 = reducer(nextState1, {
            type: REVIEW_PUBLISH_RESOLVED,
            payload: {
                review: {
                    id: 2,
                },
            },
        });

        expect(nextState2).toEqual(nextState1);
        expect(nextState2 === nextState1).toBe(true);
    });

    it('должен заменить отзыв, если он найден', () => {
        const nextState1 = reducer(state, {
            type: PAGE_LOADING_SUCCESS,
            payload: {
                myReviews: {
                    reviews: [ { id: 1 } ],
                    pagination: { page: 1 },
                },
            },
        });

        const nextState2 = reducer(nextState1, {
            type: REVIEW_PUBLISH_RESOLVED,
            payload: {
                review: {
                    id: 1,
                    foo: 'bar',
                },
            },
        });

        expect(nextState2).toEqual({
            data: {
                reviews: [ { id: 1, foo: 'bar' } ],
                pagination: { page: 1 },
            },
        });
    });
});

describe('REVIEW_REMOVE_RESOLVED', () => {
    it('не должен ничего делать, если отзыв не найден', () => {
        const nextState1 = reducer(state, {
            type: PAGE_LOADING_SUCCESS,
            payload: {
                myReviews: {
                    reviews: [ { id: 1 } ],
                    pagination: { page: 1 },
                },
            },
        });

        const nextState2 = reducer(nextState1, {
            type: REVIEW_REMOVE_RESOLVED,
            payload: {
                review: {
                    id: 2,
                },
            },
        });

        expect(nextState2).toEqual(nextState1);
        expect(nextState2 === nextState1).toBe(true);
    });

    it('должен удалить отзыв, если он найден', () => {
        const nextState1 = reducer(state, {
            type: PAGE_LOADING_SUCCESS,
            payload: {
                myReviews: {
                    reviews: [ { id: 1 }, { id: 3 } ],
                    pagination: { page: 1 },
                },
            },
        });

        const nextState2 = reducer(nextState1, {
            type: REVIEW_REMOVE_RESOLVED,
            payload: {
                reviewId: 1,
            },
        });

        expect(nextState2).toEqual({
            data: {
                reviews: [ { id: 3 } ],
                pagination: { page: 1 },
            },
        });
    });
});
