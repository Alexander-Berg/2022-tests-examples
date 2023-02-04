const { actionTypes } = require('@vertis/susanin-react');

const reducer = require('./reducer');

it('должен удалить данные из стора при переходе на другую страницу', () => {
    const state = {
        data: { report: { } },
    };

    const action = { type: actionTypes.PAGE_LOADING };

    expect(reducer(state, action)).toEqual({ data: undefined, isRefreshed: false });
});

describe('комментарии', () => {

    let comment;
    let comment2;
    beforeEach(() => {
        comment = {
            id: '1',
            block_id: 'pts',
            text: 'коммент1',
        };

        comment2 = {
            id: '2',
            block_id: 'pts',
            text: 'коммент2',
        };
    });

    it('должен правильно добавить комментарий', () => {
        const state = {
            data: { report: { comments: [ comment ] } },
        };

        const payload = comment2;
        const action = { type: 'VIN_REPORT_COMMENT_ADD', payload };

        expect(reducer(state, action)).toEqual({ data: { report: { comments: [ comment, payload ] } } });
    });

    it('должен правильно отредактировать комментарий', () => {
        const state = {
            data: { report: { comments: [
                comment2,
                comment,
            ] } },
        };

        const payload = {
            id: '2',
            block_id: 'pts',
            text: 'коммент2 был изменен',
        };
        const action = { type: 'VIN_REPORT_COMMENT_ADD', payload };

        expect(reducer(state, action)).toEqual({ data: { report: { comments: [
            payload,
            comment,
        ] } } });
    });

    it('должен правильно удалить комментарий', () => {
        const state = {
            data: { report: { comments: [
                comment,
                comment2,
            ] } },
        };

        const payload = comment;
        const action = { type: 'VIN_REPORT_COMMENT_DELETE', payload };

        expect(reducer(state, action)).toEqual({ data: { report: { comments: [
            comment2,
        ] } } });
    });
});
