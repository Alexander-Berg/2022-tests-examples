const {
    REVIEWS_GET_RESOLVED,
} = require('auto-core/react/dataDomain/reviews/types');
const reducer = require('./reducer');

it('должен правильно обработать загрузку предыдущей страницы', () => {
    const initialState = {
        data: [ { id: 3 }, { id: 4 } ],
        isFetching: true,
        pagination: { page: 2 },
    };

    const action = {
        type: REVIEWS_GET_RESOLVED,
        payload: {
            reviews: [ { id: 1 }, { id: 2 } ],
            pagination: { page: 1 },
            params: { page: 1 },
            reset: true,
        },
    };
    expect(reducer(initialState, action)).toEqual({
        data: [ { id: 1 }, { id: 2 } ],
        isFetching: false,
        pagination: { page: 1 },
        params: { page: 1 },
    });
});

it('должен правильно обработать загрузку следующей страницы', () => {
    const initialState = {
        data: [ { id: 3 }, { id: 4 } ],
        isFetching: true,
        pagination: { page: 2 },
    };

    const action = {
        type: REVIEWS_GET_RESOLVED,
        payload: {
            reviews: [ { id: 5 }, { id: 6 } ],
            pagination: { page: 3 },
            params: { page: 3 },
            reset: true,
        },
    };
    expect(reducer(initialState, action)).toEqual({
        data: [ { id: 5 }, { id: 6 } ],
        isFetching: false,
        pagination: { page: 3 },
        params: { page: 3 },
    });
});

it('должен правильно обработать подгрузку предыдущей страницы', () => {
    const initialState = {
        data: [ { id: 3 }, { id: 4 } ],
        isFetching: true,
        pagination: { page: 2 },
    };

    const action = {
        type: REVIEWS_GET_RESOLVED,
        payload: {
            reviews: [ { id: 1 }, { id: 2 } ],
            pagination: { page: 1 },
            params: { page: 1 },
            reset: false,
        },
    };
    expect(reducer(initialState, action)).toEqual({
        data: [ { id: 1 }, { id: 2 }, { id: 3 }, { id: 4 } ],
        isFetching: false,
        pagination: { page: 1 },
        params: { page: 1 },
    });
});

it('должен правильно обработать подгрузку следующей страницы', () => {
    const initialState = {
        data: [ { id: 3 }, { id: 4 } ],
        isFetching: true,
        pagination: { page: 2 },
    };

    const action = {
        type: REVIEWS_GET_RESOLVED,
        payload: {
            reviews: [ { id: 5 }, { id: 6 } ],
            pagination: { page: 3 },
            params: { page: 3 },
            reset: false,
        },
    };
    expect(reducer(initialState, action)).toEqual({
        data: [ { id: 3 }, { id: 4 }, { id: 5 }, { id: 6 } ],
        isFetching: false,
        pagination: { page: 3 },
        params: { page: 3 },
    });
});
