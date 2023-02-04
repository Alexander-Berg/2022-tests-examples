/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

const updateUrl = require('./updateUrl');

jest.mock('auto-core/react/lib/getReduxInitialState', () => {
    return {
        'default': () => ({}),
    };
});

it('не должен устанавливать catalog_filter, если он пустой', () => {
    updateUrl({ sort: 'CREATION_DATE', catalog_filter: [ {} ] });
    expect(global.location.search).toEqual('?sort=CREATION_DATE');
});

it('должен выпилить параметры rid и geo_radius', () => {
    updateUrl({ rid: [ 213 ], geo_radius: 200 });
    expect(global.location.search).toEqual('');
});

it('должен преобразовать catalog_filter в строку и установить window.location.search', () => {
    updateUrl({ catalog_filter: [ { mark: 'AUDI', model: 'Q5', generation: '8351293' } ] });
    expect(global.location.search).toEqual('?catalog_filter=mark%3DAUDI%2Cmodel%3DQ5%2Cgeneration%3D8351293');
});

it('должен установить client_id, если пришел dealer_id', () => {
    updateUrl({ dealer_id: 20101 });
    expect(global.location.search).toEqual('?client_id=20101');
});
