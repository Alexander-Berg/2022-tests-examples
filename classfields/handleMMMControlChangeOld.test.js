jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: () => Promise.resolve([]),
    };
});

const handleMMMControlChange = require('./handleMMMControlChangeOld');
const mockStore = require('autoru-frontend/mocks/mockStore').default;

let store;

it('должен добавить первый МММ', () => {
    store = mockStore({
        listing: {
            data: {
                search_parameters: {},
            },
        },
    });

    const result = store.dispatch(handleMMMControlChange([ 'AUDI' ], { name: 'mark' }, 0));
    expect(result).toEqual([ { mark: 'AUDI' } ]);
});

it('должен добавить вендора', () => {
    store = mockStore({
        listing: {
            data: {
                search_parameters: {},
            },
        },
    });

    const result = store.dispatch(handleMMMControlChange([ 'VENDOR1' ], { name: 'mark' }, 0));
    expect(result).toEqual([ { vendor: 'VENDOR1' } ]);
});

it('должен поменять марку на вендора', () => {
    store = mockStore({
        listing: {
            data: {
                search_parameters: {
                    catalog_filter: [ { mark: 'AUDI', model: '100' } ],
                },
            },
        },
    });

    const result = store.dispatch(handleMMMControlChange([ 'VENDOR1' ], { name: 'mark' }, 0));
    expect(result).toEqual([ { vendor: 'VENDOR1' } ]);
});

it('должен поменять вендора на марку', () => {
    store = mockStore({
        listing: {
            data: {
                search_parameters: {
                    catalog_filter: [ { vendor: 'VENDOR1' } ],
                },
            },
        },
    });

    const result = store.dispatch(handleMMMControlChange([ 'AUDI' ], { name: 'mark' }, 0));
    expect(result).toEqual([ { mark: 'AUDI' } ]);
});

it('должен изменить МММ', () => {
    store = mockStore({
        listing: {
            data: {
                search_parameters: {
                    catalog_filter: [ { mark: 'AUDI', model: '100' } ],
                },
            },
        },
    });

    const result = store.dispatch(handleMMMControlChange([ 'A4' ], { name: 'model' }, 0));
    expect(result).toEqual([ { mark: 'AUDI', model: 'A4' } ]);

});

it('должен удалить модель, если значение пустое', () => {
    store = mockStore({
        listing: {
            data: {
                search_parameters: {
                    catalog_filter: [ { mark: 'AUDI', model: '100' } ],
                },
            },
        },
    });

    const result = store.dispatch(handleMMMControlChange([ '' ], { name: 'model' }, 0));
    expect(result).toEqual([ { mark: 'AUDI', model: '' } ]);
});

it('должен удалить поколение, если значение пустое', () => {
    store = mockStore({
        listing: {
            data: {
                search_parameters: {
                    catalog_filter: [ { mark: 'AUDI', model: '100', generation: '123' } ],
                },
            },
        },
    });

    const result = store.dispatch(handleMMMControlChange([], { name: 'generation' }, 0));
    expect(result).toEqual([ { mark: 'AUDI', model: '100' } ]);
});

it('должен обработать множественный выбор поколений (добавление)', () => {
    store = mockStore({
        listing: {
            data: {
                search_parameters: {
                    catalog_filter: [ { mark: 'AUDI', model: '100' } ],
                },
            },
        },
    });

    const result = store.dispatch(handleMMMControlChange([ '123' ], { name: 'generation' }, 0));
    expect(result).toEqual([ { mark: 'AUDI', model: '100', generation: '123' } ]);
});

it('должен обработать множественный выбор поколений (множественное добавление)', () => {
    store = mockStore({
        listing: {
            data: {
                search_parameters: {
                    catalog_filter: [ { mark: 'AUDI', model: '100', generation: '123' } ],
                },
            },
        },
    });

    const result = store.dispatch(handleMMMControlChange([ '123', '456' ], { name: 'generation' }, 0));
    expect(result).toEqual([ { mark: 'AUDI', model: '100', generation: '123' }, { mark: 'AUDI', model: '100', generation: '456' } ]);
});

it('должен обработать множественный выбор поколений (удаление)', () => {
    store = mockStore({
        listing: {
            data: {
                search_parameters: {
                    catalog_filter: [
                        { mark: 'AUDI', model: '100', generation: '123' },
                        { mark: 'AUDI', model: '100', generation: '456' },
                    ],
                },
            },
        },
    });

    const result = store.dispatch(handleMMMControlChange([ '456' ], { name: 'generation' }, 0));
    expect(result).toEqual([ { mark: 'AUDI', model: '100', generation: '456' } ]);
});
