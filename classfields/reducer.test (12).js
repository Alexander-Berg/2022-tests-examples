const { PAGE_LOADING_SUCCESS } = require('../../actionTypes');

const reducer = require('./reducer');

let state;
beforeEach(() => {
    state = reducer(undefined, {});
});

const tagsDictionary = [
    {
        name: 'ТЭГ1',
        code: 'tag1',
    },
    {
        name: 'ТЭГ2',
        code: 'tag2',
    },
    {
        name: 'ТЭГ3',
        code: 'tag3',
    },
];

describe('PAGE_LOADING_SUCCESS', () => {
    it('не должен обработать, если нет payload.searchTagDictionary', () => {
        const nextState = reducer(state, {
            type: PAGE_LOADING_SUCCESS,
            payload: {},
        });

        expect(nextState).toEqual(state);
    });

    it('должен вернуть словарь как есть, если он есть а поиска с серч-тегами нет', () => {
        const nextState = reducer(state, {
            type: PAGE_LOADING_SUCCESS,
            payload: {
                searchTagDictionary: tagsDictionary,
            },
        });

        expect(nextState).toEqual({ data: tagsDictionary });
    });

    it('должен вернуть словарь, а первыми должны идти серч-теги из текущего поиска', () => {
        const nextState = reducer(state, {
            type: PAGE_LOADING_SUCCESS,
            payload: {
                searchTagDictionary: tagsDictionary,
                listing: {
                    search_parameters: {
                        search_tag: [ 'tag2' ],
                    },
                },
            },
        });

        expect(nextState).toEqual({ data: [
            {
                name: 'ТЭГ2',
                code: 'tag2',
                show: true,
            },
            {
                name: 'ТЭГ1',
                code: 'tag1',
            },
            {
                name: 'ТЭГ3',
                code: 'tag3',
            },
        ] });
    });

    // хз где так бывает, но судя по коду в других местах, бывает
    it('должен вернуть словарь, а первыми должны идти серч-тег из текущего поиска (строка)', () => {
        const nextState = reducer(state, {
            type: PAGE_LOADING_SUCCESS,
            payload: {
                searchTagDictionary: tagsDictionary,
                listing: {
                    search_parameters: {
                        search_tag: 'tag2',
                    },
                },
            },
        });

        expect(nextState).toEqual({ data: [
            {
                name: 'ТЭГ2',
                code: 'tag2',
                show: true,
            },
            {
                name: 'ТЭГ1',
                code: 'tag1',
            },
            {
                name: 'ТЭГ3',
                code: 'tag3',
            },
        ] });
    });

    it('должен вернуть словарь как есть, если тега в текущем поиске нет в словаре', () => {
        const nextState = reducer(state, {
            type: PAGE_LOADING_SUCCESS,
            payload: {
                searchTagDictionary: tagsDictionary,
                listing: {
                    search_parameters: {
                        search_tag: [ 'tag4' ],
                    },
                },
            },
        });

        expect(nextState).toEqual({ data: tagsDictionary });
    });
});
