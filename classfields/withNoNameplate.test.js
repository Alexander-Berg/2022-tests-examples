const withNoNameplate = require('./withNoNameplate');

it('заменяем -- в случае бесшильдовой модификации', () => {
    const searchParams = {
        catalog_filter: [ {
            mark: 'KIA',
            model: 'RIO',
            nameplate_name: '--',
        } ],
    };
    withNoNameplate(searchParams);
    expect(searchParams).toEqual({
        catalog_filter: [ {
            mark: 'KIA',
            model: 'RIO',
            nameplate_name: 'rio',
        } ],
    });
});

it('ничего не делаем, если нет модели', () => {
    const searchParams = {
        catalog_filter: [ {
            mark: 'KIA',
        } ],
    };
    withNoNameplate(searchParams);
    expect(searchParams).toEqual({
        catalog_filter: [ {
            mark: 'KIA',
        } ],
    });
});

it('ничего не делаем, если нет шильда', () => {
    const searchParams = {
        catalog_filter: [ {
            mark: 'KIA',
            model: 'RIO',
        } ],
    };
    withNoNameplate(searchParams);
    expect(searchParams).toEqual({
        catalog_filter: [ {
            mark: 'KIA',
            model: 'RIO',
        } ],
    });
});
