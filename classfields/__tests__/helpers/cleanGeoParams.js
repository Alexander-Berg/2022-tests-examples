const cleanGeoParams = require('../../helpers/cleanGeoParams');

describe('cleanGeoParams', () => {
    it('Оставить только район, убрать остальные параметры', () => {
        const params = {
            subLocality: 1,
            metroGeoId: 2,
            streetId: 3
        };
        const result = cleanGeoParams(params);

        expect(result).toEqual({
            subLocality: 1
        });
    });

    it('Оставить только метро, убрать остальные параметры', () => {
        const params = {
            metroGeoId: 2,
            streetId: 3,
            streetName: 'keka'
        };
        const result = cleanGeoParams(params);

        expect(result).toEqual({
            metroGeoId: 2
        });
    });

    it('Оставить только улицу, убрать остальные параметры', () => {
        const params = {
            streetId: 3,
            streetName: 'keka'
        };
        const result = cleanGeoParams(params);

        expect(result).toEqual({
            streetId: 3,
            streetName: 'keka'
        });
    });

    it('Один параметр не удаляется', () => {
        const params1 = {
            subLocality: 1
        };
        const result1 = cleanGeoParams(params1);

        expect(result1).toEqual({
            subLocality: 1
        });

        const params2 = {
            metroGeoId: 2
        };
        const result2 = cleanGeoParams(params2);

        expect(result2).toEqual({
            metroGeoId: 2
        });

        const params3 = {
            streetId: 3
        };
        const result3 = cleanGeoParams(params3);

        expect(result3).toEqual({
            streetId: 3
        });
    });
});
