const parseRegionFromUrl = require('./parseRegionFromUrl');

it('возвращает регион и остальной путь для url с регионом', () => {
    const {
        region,
        url,
    } = parseRegionFromUrl('/moskva/cool/cars/');

    expect(url).toEqual('/cool/cars/');
    expect(region).toEqual('moskva');
});

it('возвращает путь для url без региона', () => {
    const {
        region,
        url,
    } = parseRegionFromUrl('/cool/cars/');

    expect(url).toEqual('/cool/cars/');
    expect(region).toBeUndefined();
});

it('возвращает регион и пустой путь для url только с регионом', () => {
    const {
        region,
        url,
    } = parseRegionFromUrl('/moskva/');

    expect(url).toEqual('/');
    expect(region).toEqual('moskva');
});

it('возвращает регион и пустой путь для url только с регионом бещ слеша', () => {
    const {
        region,
        url,
    } = parseRegionFromUrl('/moskva');

    expect(url).toEqual('/');
    expect(region).toEqual('moskva');
});

it('возвращает / для корня', () => {
    const {
        region,
        url,
    } = parseRegionFromUrl('/');

    expect(url).toEqual('/');
    expect(region).toBeUndefined();
});
