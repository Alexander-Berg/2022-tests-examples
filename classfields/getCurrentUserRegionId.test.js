const getCurrentUserRegionId = require('auto-core/react/dataDomain/geo/selectors/getCurrentUserRegionId');

it('возвращает сохраненные регионы', () => {
    const result = getCurrentUserRegionId({
        geo: {
            gids: [
                2,
                3,
            ],
            regionByIp: 5,
        },
    });

    expect(result).toEqual([
        2,
        3,
    ]);
});

it('возвращает регион по ip, если нет сохраненных регионы', () => {
    const result = getCurrentUserRegionId({
        geo: {
            regionByIp: 5,
        },
    });

    expect(result).toEqual([
        5,
    ]);
});

it('возвращает пустой массив, если ничего по регионам нет', () => {
    const result = getCurrentUserRegionId({
        geo: {},
    });

    expect(result).toEqual([]);
});
