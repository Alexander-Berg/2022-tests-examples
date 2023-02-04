const convertOldListingParamsToNew = require('./convertOldListingParamsToNew');

const TESTS = require('./convertSearcherParams.testcases');

TESTS.forEach(testCase => {
    if (!testCase.searcher) {
        return;
    }

    // eslint-disable-next-line max-len
    it(`должен сконвертировать ${ JSON.stringify(testCase.searcher) } в ${ JSON.stringify(testCase.public_api) }${ testCase.category ? ' для ' + testCase.category : '' }`, () => {
        expect(convertOldListingParamsToNew(testCase.searcher, testCase.category)).toEqual(testCase.public_api);
    });
});

it('должен вернуть null, если конвертер in_stock ничего не поменял', () => {
    expect(convertOldListingParamsToNew({ in_stock: 'ANY_STOCK' })).toBeNull();
});

it('должен удалить photo, если "photo=true"', () => {
    expect(convertOldListingParamsToNew({ photo: 'true' })).toBeNull();
});

it('должен преобразовать массив "catalog_equipment[]=light-interior"', () => {
    expect(convertOldListingParamsToNew({ 'catalog_equipment[]': 'light-interior' })).toEqual({
        catalog_equipment: [ 'light-interior' ],
    });
});

it('должен преобразовать dealer_org_type=[1,2,3,5] в COMMERCIAL', () => {
    expect(convertOldListingParamsToNew({ dealer_org_type: [ '1', '2', '3', '5' ] })).toEqual({
        seller_group: 'COMMERCIAL',
    });
});

it('если moto_category не менялся, то convertOldListingParamsToNew должен вернуть false', () => {
    expect(convertOldListingParamsToNew({ moto_category: 'MOTORCYCLE' })).toBeNull();
});

it('если trucks_category не менялся, то convertOldListingParamsToNew должен вернуть false', () => {
    expect(convertOldListingParamsToNew({ trucks_category: 'LCV' })).toBeNull();
});

it('в price_from нужно удалять пробелы', () => {
    const params = { price_from: '1 000 000' };
    expect(convertOldListingParamsToNew(params)).toEqual({ price_from: '1000000' });
});

it('в price_to нужно удалять пробелы', () => {
    const params = { price_to: '1 000 000' };
    expect(convertOldListingParamsToNew(params)).toEqual({ price_to: '1000000' });
});

it('правильные цены не должны аффектить', () => {
    const params = { price_to: '1000000' };
    expect(convertOldListingParamsToNew(params)).toBeNull();
});

describe('strokes', () => {
    it('не должен менять валидные параметры', () => {
        expect(convertOldListingParamsToNew({ strokes: [ 'STROKES_2', 'STROKES_4' ] })).toBeNull();
    });
});

describe('loading -> loading_from/loading_to', () => {
    // преобразование работает в одну сторону, потому что в новом интерфейсе это не пресеты

    it('должен преобразовать loading=0_1500 в { loading_to: 1500 }', () => {
        expect(convertOldListingParamsToNew({ loading: '0_1500' })).toEqual({
            loading_to: 1500,
        });
    });

    it('должен преобразовать loading=3000_5000 в { loading_from: 3000, loading_to: 5000 }', () => {
        expect(convertOldListingParamsToNew({ loading: '3000_5000' })).toEqual({
            loading_from: 3000,
            loading_to: 5000,
        });
    });

    it('должен преобразовать loading=8000_1000000 в { loading_from: 8000 }', () => {
        expect(convertOldListingParamsToNew({ loading: '8000_1000000' })).toEqual({
            loading_from: 8000,
        });
    });

    it('должен из массива взять только первое значение', () => {
        expect(convertOldListingParamsToNew({ loading: [ '1500_3000', '5000_8000' ] })).toEqual({
            loading_from: 1500,
            loading_to: 3000,
        });
    });
});

describe('state -> damage_group', () => {
    it('должен преобразовать state=BEATEN в damage_group=BEATEN', () => {
        expect(convertOldListingParamsToNew({ state: 'BEATEN' })).toEqual({
            section: 'all',
            damage_group: 'BEATEN',
        });
    });

    it('должен преобразовать state=[ BEATEN, NEW, USED ] в damage_group=ANY', () => {
        expect(convertOldListingParamsToNew({ state: [ 'BEATEN', 'NEW', 'USED' ] })).toEqual({
            section: 'all',
            damage_group: 'ANY',
        });
    });
});

describe('mark_model_nameplate -> catalog_filter', () => {
    it('должен преобразовать mark_model_nameplate=AUDI в catalog_filter=[{mark:AUDI}]', () => {
        expect(convertOldListingParamsToNew({ mark_model_nameplate: 'AUDI' })).toEqual({
            catalog_filter: [ { mark: 'AUDI' } ],
        });
    });

    it('должен преобразовать mark_model_nameplate=["AUDI"] в catalog_filter=[{mark:AUDI}]', () => {
        expect(convertOldListingParamsToNew({ mark_model_nameplate: [ 'AUDI' ] })).toEqual({
            catalog_filter: [ { mark: 'AUDI' } ],
        });
    });
});

describe('moto_type', () => {
    it('должен обработать moto_type как строка', () => {
        expect(convertOldListingParamsToNew({
            moto_category: 'motorcycle',
            moto_type: 'TOURIST_ENDURO',
        })).toEqual({
            moto_category: 'MOTORCYCLE',
            moto_type: [ 'TOURIST_ENDURO' ],
        });
    });
});

describe('trailer_type', () => {
    it('должен удалить групповые значения', () => {
        expect(convertOldListingParamsToNew({
            trailer_type: [
                'SWAP_BODY_ALL',
                'SWAP_BODY_ALL_BULK_CARGO',
                'SEMI_TRAILER_ALL',
                'SEMI_TRAILER_ALL_ST_ASSORTMENT',
                'TRAILER_ALL',
                'TRAILER_ALL_ADVERTIZING',
            ],
        })).toEqual({
            trailer_type: [ 'BULK_CARGO', 'ST_ASSORTMENT', 'ADVERTIZING' ],
        });
    });
});

describe('удаление вендора not-chinese', () => {
    it('должен поменять вендора "кроме китайских" на исключение вендора "китайские" и исключения не было', () => {
        expect(convertOldListingParamsToNew({
            catalog_filter: [ { vendor: 'VENDOR15' }, { vendor: 'VENDOR1' } ],
        })).toEqual({
            catalog_filter: [ { vendor: 'VENDOR1' } ],
            exclude_catalog_filter: [ { vendor: 'VENDOR10' } ],
        });
    });

    it('должен поменять вендора "кроме китайских" на исключение вендора "китайские"', () => {
        expect(convertOldListingParamsToNew({
            catalog_filter: [ { vendor: 'VENDOR15' } ],
            exclude_catalog_filter: [ { vendor: 'VENDOR2' } ],
        })).toEqual({
            catalog_filter: [],
            exclude_catalog_filter: [ { vendor: 'VENDOR2' }, { vendor: 'VENDOR10' } ],
        });
    });

    it('должен поменять исключение вендора "кроме китайских" на вендора "китайские"', () => {
        expect(convertOldListingParamsToNew({
            exclude_catalog_filter: [ { vendor: 'VENDOR15' }, { vendor: 'VENDOR1' } ],
        })).toEqual({
            exclude_catalog_filter: [ { vendor: 'VENDOR1' } ],
            catalog_filter: [ { vendor: 'VENDOR10' } ],
        });
    });

    it('не должен редиректить, если в фильтрах нет "кроме китайских"', () => {
        expect(convertOldListingParamsToNew({
            exclude_catalog_filter: [ { mark: 'CHEVROLET', model: 'CAMARO' } ],
        })).toBeNull();
    });

    it('должен сделать редирект, если в урле марка vendor-not-chinese', () => {
        expect(convertOldListingParamsToNew({
            catalog_filter: [ { mark: 'VENDOR-NOT-CHINESE' } ],
        })).toEqual({
            catalog_filter: [],
            exclude_catalog_filter: [ { vendor: 'VENDOR10' } ],
        });
    });
});

describe('haggle', () => {
    it('должен преобразовать haggle=POSSIBLE в haggle=HAGGLE_POSSIBLE для коммтс', () => {
        expect(convertOldListingParamsToNew({ haggle: 'POSSIBLE' }, 'trucks')).toEqual({
            haggle: 'HAGGLE_POSSIBLE',
        });
    });

    it('не должен преобразовать haggle=HAGGLE_POSSIBLE', () => {
        expect(convertOldListingParamsToNew({ haggle: 'HAGGLE_POSSIBLE' }, 'trucks')).toBeNull();
    });
});
