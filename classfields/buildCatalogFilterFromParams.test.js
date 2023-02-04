const buildCatalogFilterFromParams = require('./buildCatalogFilterFromParams');

it('должен вернуть пустой объект, если в параметрах нет mmm', () => {
    expect(buildCatalogFilterFromParams({ foo: 'bar' })).toEqual({});
});

it('должен правильно построить mmm, если есть марка', () => {
    expect(buildCatalogFilterFromParams({
        mark: 'audi',
    })).toEqual({
        mark: 'AUDI',
    });
});

it('должен правильно построить mmm, если есть марка и модель', () => {
    expect(buildCatalogFilterFromParams({
        mark: 'audi',
        model: 'a4',
    })).toEqual({
        mark: 'AUDI',
        model: 'A4',
    });
});

it('должен правильно построить mmm, если есть марка-модель-шильдик-поколение-конфигурация-комплектация', () => {
    expect(buildCatalogFilterFromParams({
        mark: 'audi',
        model: 'a4',
        nameplate_name: 'ETA_SHILDIK1',
        super_gen: '123',
        configuration_id: '456',
        tech_param_id: '789',
    })).toEqual({
        mark: 'AUDI',
        model: 'A4',
        nameplate_name: 'eta_shildik1',
        generation: '123',
        configuration: '456',
        tech_param: '789',
    });
});

it('должен правильно построить mmm, если есть вендор', () => {
    expect(buildCatalogFilterFromParams({
        mark: 'vendor1',
    })).toEqual({
        vendor: 'VENDOR1',
    });
});
