import getDefaultGeoRadius from './getDefaultGeoRadius';

it('должен вернуть 200 для category=cars', () => {
    expect(getDefaultGeoRadius({
        pageParams: { category: 'cars' },
        geoParentsIds: [],
    })).toEqual(200);
});

it('должен вернуть 500 для category=cars, Новосибирска', () => {
    expect(getDefaultGeoRadius({
        pageParams: { category: 'cars' },
        geoParentsIds: [ 121037, 11316, 59, 225 ],
    })).toEqual(500);
});

it('должен вернуть 500 для category=cars, Новосибирской области', () => {
    expect(getDefaultGeoRadius({
        pageParams: { category: 'cars' },
        geoParentsIds: [ 11316, 59, 225 ],
    })).toEqual(500);
});

it('должен вернуть 0 для category=cars, pageType=dealers', () => {
    expect(getDefaultGeoRadius({
        pageParams: { category: 'cars' },
        pageType: 'dealers',
        geoParentsIds: [],
    })).toEqual(0);
});

it('должен вернуть 500 для category=trucks, без trucks_category', () => {
    expect(getDefaultGeoRadius({
        pageParams: { category: 'trucks' },
        geoParentsIds: [],
    })).toEqual(500);
});

it('должен вернуть 500 для category=trucks, trucks_category=TRUCK', () => {
    expect(getDefaultGeoRadius({
        pageParams: { category: 'trucks', trucks_category: 'TRUCK' },
        geoParentsIds: [],
    })).toEqual(500);
});

it('должен вернуть 200 для category=trucks, trucks_category=LCV', () => {
    expect(getDefaultGeoRadius({
        pageParams: { category: 'trucks', trucks_category: 'LCV' },
        geoParentsIds: [],
    })).toEqual(200);
});

it('должен вернуть 200 для category=trucks, trucks_category=LCV, Новосибирск', () => {
    expect(getDefaultGeoRadius({
        pageParams: { category: 'trucks', trucks_category: 'LCV' },
        geoParentsIds: [ 121037, 11316, 59, 225 ],
    })).toEqual(200);
});

it('должен вернуть 1000 для category=cars с экспом SEARCHER_VS_1240_NO_HOME_TIER', () => {
    expect(getDefaultGeoRadius({
        pageParams: { category: 'cars' },
        geoParentsIds: [],
        has1000kmExp: true,
    })).toEqual(1000);
});

it('должен вернуть 200 для category не cars с экспом SEARCHER_VS_1240_NO_HOME_TIER', () => {
    expect(getDefaultGeoRadius({
        pageParams: { category: 'moto' },
        geoParentsIds: [],
        has1000kmExp: true,
    })).toEqual(200);
});

it('должен вернуть 200 если нет категории в экспе SEARCHER_VS_1240_NO_HOME_TIER', () => {
    expect(getDefaultGeoRadius({
        pageParams: {},
        geoParentsIds: [],
        has1000kmExp: true,
    })).toEqual(200);
});
