jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: () => Promise.resolve([]),
    };
});

const handleChangedControl = require('./handleChangedControl');
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const tagsDictionary = {
    data: [
        {
            code: 'tag1',
            tag: {
                applicability: 'ALL',
            },
        },
        {
            code: 'tag2',
            tag: {
                applicability: 'NEW',
            },
        },
        {
            code: 'tag3',
            tag: {
                applicability: 'USED',
            },
        },
    ],
};
let store;

describe('section', () => {
    store = mockStore({
        listing: {
            data: {
                search_parameters: {
                    search_tag: [ 'tag1', 'tag2', 'tag3', 'tag4' ],
                },
            },
        },
        searchTagDictionary: tagsDictionary,
    });

    it('должен оставить все теги при переходе в all', () => {
        const result = store.dispatch(handleChangedControl([ { newValue: 'all', control: { name: 'section' } } ]));
        expect(result).toEqual({ search_tag: [ 'tag1', 'tag2', 'tag3', 'tag4' ], section: 'all' });
    });

    it('должен удолить неподходящие теги при переходе в new', () => {
        const result = store.dispatch(handleChangedControl([ { newValue: 'new', control: { name: 'section' } } ]));
        expect(result).toEqual({ search_tag: [ 'tag1', 'tag2', 'tag4' ], section: 'new' });
    });

    it('должен удолить неподходящие теги при переходе в used', () => {
        const result = store.dispatch(handleChangedControl([ { newValue: 'used', control: { name: 'section' } } ]));
        expect(result).toEqual({ search_tag: [ 'tag1', 'tag3', 'tag4' ], section: 'used' });
    });
});

describe('search_tag_cert', () => {
    it('должен добавить тег производителя', () => {
        store = mockStore({
            listing: {
                data: {
                    search_parameters: {
                        search_tag: [ 'tag1' ],
                    },
                },
            },
        });
        const result = store.dispatch(handleChangedControl([ { newValue: 'cert_tag', control: { name: 'search_tag_cert', mode: 'add' } } ]));
        expect(result).toEqual({ search_tag: [ 'tag1', 'cert_tag' ] });
    });

    it('должен удолить тег производителя', () => {
        store = mockStore({
            listing: {
                data: {
                    search_parameters: {
                        search_tag: [ 'tag1', 'cert_tag' ],
                    },
                },
            },
        });
        const result = store.dispatch(handleChangedControl([ { newValue: 'cert_tag', control: { name: 'search_tag_cert', mode: 'delete' } } ]));
        expect(result).toEqual({ search_tag: [ 'tag1' ] });
    });
});

describe('search_tag_panorama', () => {
    it('должен добавить тег про панораму', () => {
        store = mockStore({
            listing: {
                data: {
                    search_parameters: {
                        search_tag: [ 'tag1' ],
                    },
                },
            },
        });
        const result = store.dispatch(handleChangedControl([ { newValue: 'external_panoramas', control: { name: 'search_tag_panorama', mode: 'add' } } ]));
        expect(result).toEqual({ search_tag: [ 'tag1', 'external_panoramas' ] });
    });

    it('должен удолить тег про панораму', () => {
        store = mockStore({
            listing: {
                data: {
                    search_parameters: {
                        search_tag: [ 'tag1', 'external_panoramas' ],
                    },
                },
            },
        });
        const result = store.dispatch(handleChangedControl([ { newValue: 'external_panoramas', control: { name: 'search_tag_panorama', mode: 'delete' } } ]));
        expect(result).toEqual({ search_tag: [ 'tag1' ] });
    });
});

describe('search_tag_dict', () => {
    it('должен добавить теги (есть тег из словаря и другой)', () => {
        store = mockStore({
            listing: {
                data: {
                    search_parameters: {
                        search_tag: [ 'tag2', 'other_tag' ],
                    },
                },
            },
            searchTagDictionary: tagsDictionary,
        });
        const result = store.dispatch(handleChangedControl([ { newValue: [ 'tag1' ], control: { name: 'search_tag_dict' } } ]));
        expect(result).toEqual({ search_tag: [ 'other_tag', 'tag1' ] });
    });

    it('должен добавить теги (были только теги из словаря)', () => {
        store = mockStore({
            listing: {
                data: {
                    search_parameters: {
                        search_tag: [ 'tag2' ],
                    },
                },
            },
            searchTagDictionary: tagsDictionary,
        });
        const result = store.dispatch(handleChangedControl([ { newValue: [ 'tag1' ], control: { name: 'search_tag_dict' } } ]));
        expect(result).toEqual({ search_tag: [ 'tag1' ] });
    });

    it('должен добавить теги (был только несловарный тег)', () => {
        store = mockStore({
            listing: {
                data: {
                    search_parameters: {
                        search_tag: [ 'other_tag' ],
                    },
                },
            },
            searchTagDictionary: tagsDictionary,
        });
        const result = store.dispatch(handleChangedControl([ { newValue: [ 'tag1' ], control: { name: 'search_tag_dict' } } ]));
        expect(result).toEqual({ search_tag: [ 'other_tag', 'tag1' ] });
    });

    it('должен добавить теги (не было тегов)', () => {
        store = mockStore({
            listing: {
                data: {
                    search_parameters: {},
                },
            },
            searchTagDictionary: tagsDictionary,
        });
        const result = store.dispatch(handleChangedControl([ { newValue: [ 'tag1' ], control: { name: 'search_tag_dict' } } ]));
        expect(result).toEqual({ search_tag: [ 'tag1' ] });
    });
});

describe('catalog_equipment', () => {
    it('должен добавить опцию', () => {
        store = mockStore({
            listing: {
                data: {
                    search_parameters: {
                        catalog_equipment: [ 'led-lights,laser-lights', 'ptf' ],
                    },
                },
            },
        });
        const result = store.dispatch(handleChangedControl(
            [ {
                newValue: [ 'laser-lights' ],
                control: {
                    availableValues: [ 'xenon', 'laser-lights', 'led-lights' ],
                    name: 'catalog_equipment',
                    value: [ 'laser-lights' ],
                },
            } ],
        ));
        expect(result).toEqual({ catalog_equipment: [ 'laser-lights', 'ptf' ] });
    });

    it('должен полностью заменить опции, если есть флаг total', () => {
        store = mockStore({
            listing: {
                data: {
                    search_parameters: {
                        catalog_equipment: [ 'led-lights,laser-lights', 'ptf' ],
                    },
                },
            },
        });
        const result = store.dispatch(handleChangedControl(
            [ {
                newValue: [ 'laser-lights' ],
                control: {
                    name: 'catalog_equipment',
                    total: true,
                },
            } ],
        ));
        expect(result).toEqual({ catalog_equipment: [ 'laser-lights' ] });
    });
});
