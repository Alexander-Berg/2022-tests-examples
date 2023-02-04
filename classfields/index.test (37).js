const makeDirectText = require('app/lib/direct-text/index');

describe('Direct text', () => {
    it('should be properly generated', () => {
        expect(
            makeDirectText({
                category: 'APARTMENT',
                roomsTotal: '2',
                newFlat: 'YES',
                type: 'SELL'
            }, 'в Москве')
        ).toBe('купить двухкомнатную квартиру в новостройке в Москве');

        expect(
            makeDirectText({
                category: 'APARTMENT',
                roomsTotal: '2',
                newFlat: 'YES',
                type: 'RENT'
            }, 'в Санкт-Петербурге')
        ).toBe('снять двухкомнатную квартиру в аренду в Санкт-Петербурге');

        expect(
            makeDirectText({
                category: 'APARTMENT',
                roomsTotal: [ '2', '3' ],
                newFlat: 'YES',
                type: 'RENT'
            }, 'в Санкт-Петербурге')
        ).toBe('снять двухкомнатную трехкомнатную квартиру в аренду в Санкт-Петербурге');

        expect(
            makeDirectText({
                category: 'APARTMENT',
                roomsTotal: [ '2', '3' ],
                newFlat: 'YES',
                type: 'RENT',
                rentTime: 'SHORT'
            }, 'в Санкт-Петербурге')
        ).toBe('снять двухкомнатную трехкомнатную квартиру в аренду посуточно в Санкт-Петербурге');

        expect(
            makeDirectText({
                category: 'APARTMENT',
                roomsTotal: [ '2', '3' ],
                newFlat: 'YES',
                type: 'SELL',
                rentTime: 'SHORT'
            }, 'в Казани')
        ).toBe('купить двухкомнатную трехкомнатную квартиру в новостройке в Казани');

        expect(
            makeDirectText({
                category: 'APARTMENT',
                roomsTotal: 'OPEN_PLAN',
                newFlat: 'YES',
                type: 'SELL',
                rentTime: 'SHORT'
            }, 'в Казани')
        ).toBe('купить квартиру свободной планировки в новостройке в Казани');

        expect(
            makeDirectText({
                category: 'GARAGE',
                newFlat: 'YES',
                type: 'SELL'
            }, 'в Казани')
        ).toBe('купить гараж в Казани');

        expect(
            makeDirectText({
                category: 'GARAGE',
                garageType: 'BOX',
                type: 'SELL'
            }, 'в Казани')
        ).toBe('купить бокс в Казани');

        expect(
            makeDirectText({
                category: 'COMMERCIAL',
                garageType: 'BOX',
                type: 'SELL'
            }, 'в Казани')
        ).toBe('купить коммерческую недвижимость в Казани');

        expect(
            makeDirectText({
                category: 'COMMERCIAL',
                commercialType: 'OFFICE',
                garageType: 'BOX',
                type: 'SELL'
            }, 'в Казани')
        ).toBe('купить офис в Казани');
    });

    it('should handle options', function() {
        expect(
            makeDirectText({
                category: 'COMMERCIAL',
                commercialType: 'OFFICE',
                garageType: 'BOX',
                type: 'SELL'
            },
            'в Москве',
            {
                alwaysShowCommercial: true
            })
        ).toBe('купить коммерческую недвижимость офис в Москве');

        expect(
            makeDirectText({
                category: 'APARTMENT',
                roomsTotal: '3',
                type: 'SELL'
            },
            'в Москве',
            {
                hideRoomsTotal: true
            })
        ).toBe('купить квартиру в Москве');

        expect(
            makeDirectText({
                category: 'GARAGE',
                garageType: 'BOX',
                type: 'SELL'
            },
            'в Москве',
            {
                hideRoomsTotal: true
            })
        ).toBe('купить гараж в Москве');

        expect(
            makeDirectText({
                category: 'COMMERCIAL',
                commercialType: 'HOTEL',
                type: 'SELL'
            },
            'в Москве',
            {
                hideRoomsTotal: true
            })
        ).toBe('купить коммерческую недвижимость в Москве');

        expect(
            makeDirectText({
                category: 'HOUSE',
                houseType: 'PARTHOUSE',
                type: 'SELL'
            },
            'в Москве',
            {
                hideRoomsTotal: true
            })
        ).toBe('купить дом в Москве');
    });

    it('should handle invalid category', function() {
        expect(
            makeDirectText({
                category: 'INVALID',
                type: 'SELL'
            }, 'в Москве')
        ).toBe('купить недвижимость в Москве');
    });

    it('should handle invalid garageType / commercialType', function() {
        expect(
            makeDirectText({
                category: 'GARAGE',
                type: 'SELL',
                garageType: ''
            }, 'в Москве')
        ).toBe('купить гараж в Москве');

        expect(
            makeDirectText({
                category: 'GARAGE',
                type: 'SELL',
                garageType: 'BAD'
            }, 'в Москве')
        ).toBe('купить гараж в Москве');

        expect(
            makeDirectText({
                category: 'COMMERCIAL',
                type: 'SELL',
                commercialType: 'BAD'
            }, 'в Москве')
        ).toBe('купить коммерческую недвижимость в Москве');

        expect(
            makeDirectText({
                category: 'APARTMENT',
                type: 'SELL',
                roomsTotal: 'BAD'
            }, 'в Москве')
        ).toBe('купить квартиру в Москве');
    });

    it('@cofemiss tests', () => {
        expect(
            makeDirectText({
                type: 'SELL',
                category: 'LOT'
            }, 'в Москве')
        ).toBe('купить участок в Москве');

        expect(
            makeDirectText({
                type: 'SELL',
                category: 'ROOMS'
            }, 'в Москве')
        ).toBe('купить комнату в Москве');

        expect(
            makeDirectText({
                type: 'RENT',
                category: 'ROOMS',
                rentTime: 'SHORT'
            }, 'в Москве')
        ).toBe('снять комнату в аренду посуточно в Москве');

        expect(
            makeDirectText({
                type: 'RENT',
                category: 'ROOMS',
                rentTime: 'LARGE'
            }, 'в Москве')
        ).toBe('снять комнату в аренду в Москве');

        expect(
            makeDirectText({
                type: 'SELL',
                category: 'GARAGE'
            }, 'в Уфе')
        ).toBe('купить гараж в Уфе');

        expect(
            makeDirectText({
                garageType: 'BOX',
                type: 'SELL',
                category: 'GARAGE'
            }, 'в Уфе')
        ).toBe('купить бокс в Уфе');

        expect(
            makeDirectText({
                garageType: [ 'BOX', 'GARAGE', 'PARKING_PLACE' ],
                type: 'SELL',
                category: 'GARAGE'
            }, 'в Уфе')
        ).toBe('купить бокс гараж машиноместо в Уфе');

        expect(
            makeDirectText({
                type: 'RENT',
                category: 'GARAGE',
                rentTime: 'LARGE'
            }, 'в Уфе')
        ).toBe('снять гараж в Уфе');

        expect(
            makeDirectText({
                garageType: 'BOX',
                type: 'RENT',
                category: 'GARAGE',
                rentTime: 'LARGE'
            }, 'в Уфе')
        ).toBe('снять бокс в Уфе');

        expect(
            makeDirectText({
                garageType: [ 'BOX', 'GARAGE', 'PARKING_PLACE' ],
                type: 'RENT',
                category: 'GARAGE',
                rentTime: 'LARGE'
            }, 'в Уфе')
        ).toBe('снять бокс гараж машиноместо в Уфе');

        expect(
            makeDirectText({
                type: 'SELL',
                category: 'APARTMENT'
            }, 'в Москве'),
        ).toBe('купить квартиру в Москве');

        expect(
            makeDirectText({
                type: 'SELL',
                category: 'APARTMENT',
                newFlat: 'YES'
            }, 'в Москве')
        ).toBe('купить квартиру в новостройке в Москве');

        expect(
            makeDirectText({
                roomsTotal: 'STUDIO',
                type: 'SELL',
                category: 'APARTMENT',
                newFlat: 'YES'
            }, 'в Москве')
        ).toBe('купить студию в новостройке в Москве');

        expect(
            makeDirectText({
                roomsTotal: 'OPEN_PLAN',
                type: 'SELL',
                category: 'APARTMENT',
                newFlat: 'YES'
            }, 'в Москве')
        ).toBe('купить квартиру свободной планировки в новостройке в Москве');

        expect(
            makeDirectText({
                roomsTotal: [ '1', 'STUDIO' ],
                type: 'SELL',
                category: 'APARTMENT',
                newFlat: 'YES'
            }, 'в Москве')
        ).toBe('купить однокомнатную студию квартиру в новостройке в Москве');

        expect(
            makeDirectText({
                type: 'RENT',
                category: 'APARTMENT',
                rentTime: 'LARGE'
            }, 'в Москве')
        ).toBe('снять квартиру в аренду в Москве');

        expect(
            makeDirectText({
                roomsTotal: 'STUDIO',
                type: 'RENT',
                category: 'APARTMENT',
                rentTime: 'LARGE'
            }, 'в Москве')
        ).toBe('снять студию в аренду в Москве');

        expect(
            makeDirectText({
                roomsTotal: 'OPEN_PLAN',
                type: 'RENT',
                category: 'APARTMENT',
                rentTime: 'LARGE'
            }, 'в Москве и Московской области')
        ).toBe('снять квартиру свободной планировки в аренду в Москве и Московской области');

        expect(
            makeDirectText({
                roomsTotal: [ '1', '2' ],
                type: 'RENT',
                category: 'APARTMENT',
                rentTime: 'LARGE'
            }, 'в Москве и Московской области')
        ).toBe('снять однокомнатную двухкомнатную квартиру в аренду в Москве и Московской области');

        expect(
            makeDirectText({
                type: 'RENT',
                category: 'APARTMENT',
                rentTime: 'SHORT'
            }, 'в Москве')
        ).toBe('снять квартиру в аренду посуточно в Москве');

        expect(
            makeDirectText({
                roomsTotal: 'STUDIO',
                type: 'RENT',
                category: 'APARTMENT',
                rentTime: 'SHORT'
            }, 'в Москве')
        ).toBe('снять студию в аренду посуточно в Москве');

        expect(
            makeDirectText({
                roomsTotal: 'OPEN_PLAN',
                type: 'RENT',
                category: 'APARTMENT',
                rentTime: 'SHORT'
            }, 'в Москве и Московской области')
        ).toBe('снять квартиру свободной планировки в аренду посуточно в Москве и Московской области');

        expect(
            makeDirectText({
                roomsTotal: [ '1', '2' ],
                type: 'RENT',
                category: 'APARTMENT',
                rentTime: 'SHORT'
            }, 'в Москве и Московской области')
        ).toBe('снять однокомнатную двухкомнатную квартиру в аренду посуточно в Москве и Московской области');

        expect(
            makeDirectText({
                type: 'SELL',
                category: 'HOUSE'
            }, 'в Кронштадте')
        ).toBe('купить дом в Кронштадте');

        expect(
            makeDirectText({
                houseType: 'HOUSE',
                type: 'SELL',
                category: 'HOUSE'
            }, 'в Кронштадте')
        ).toBe('купить дом в Кронштадте');

        expect(
            makeDirectText({
                houseType: 'PARTHOUSE',
                type: 'SELL',
                category: 'HOUSE'
            }, 'в Кронштадте')
        ).toBe('купить часть дома в Кронштадте');

        expect(
            makeDirectText({
                houseType: [ 'TOWNHOUSE', 'DUPLEX' ],
                type: 'SELL',
                category: 'HOUSE'
            }, 'в Кронштадте')
        ).toBe('купить таунхаус дуплекс в Кронштадте');

        expect(
            makeDirectText({
                type: 'RENT',
                category: 'HOUSE',
                rentTime: 'LARGE'
            }, 'в Кронштадте')
        ).toBe('снять дом в аренду в Кронштадте');

        expect(
            makeDirectText({
                houseType: 'HOUSE',
                type: 'RENT',
                category: 'HOUSE',
                rentTime: 'LARGE'
            }, 'в Кронштадте')
        ).toBe('снять дом в аренду в Кронштадте');

        expect(
            makeDirectText({
                houseType: 'PARTHOUSE',
                type: 'RENT',
                category: 'HOUSE',
                rentTime: 'LARGE'
            }, 'в Кронштадте')
        ).toBe('снять часть дома в аренду в Кронштадте');

        expect(
            makeDirectText({
                houseType: [ 'TOWNHOUSE', 'DUPLEX' ],
                type: 'RENT',
                category: 'HOUSE',
                rentTime: 'LARGE'
            }, 'в Кронштадте')
        ).toBe('снять таунхаус дуплекс в аренду в Кронштадте');

        expect(
            makeDirectText({
                type: 'RENT',
                category: 'HOUSE',
                rentTime: 'SHORT'
            }, 'в Кронштадте')
        ).toBe('снять дом в аренду посуточно в Кронштадте');

        expect(
            makeDirectText({
                houseType: 'HOUSE',
                type: 'RENT',
                category: 'HOUSE',
                rentTime: 'SHORT'
            }, 'в Кронштадте')
        ).toBe('снять дом в аренду посуточно в Кронштадте');

        expect(
            makeDirectText({
                houseType: 'PARTHOUSE',
                type: 'RENT',
                category: 'HOUSE',
                rentTime: 'SHORT'
            }, 'в Кронштадте')
        ).toBe('снять часть дома в аренду посуточно в Кронштадте');

        expect(
            makeDirectText({
                houseType: [ 'TOWNHOUSE', 'DUPLEX' ],
                type: 'RENT',
                category: 'HOUSE',
                rentTime: 'SHORT'
            }, 'в Кронштадте')
        ).toBe('снять таунхаус дуплекс в аренду посуточно в Кронштадте');

        expect(
            makeDirectText({
                type: 'SELL',
                category: 'COMMERCIAL'
            }, 'в Кронштадте')
        ).toBe('купить коммерческую недвижимость в Кронштадте');

        expect(
            makeDirectText({
                commercialType: 'OFFICE',
                type: 'SELL',
                category: 'COMMERCIAL'
            }, 'в Кронштадте')
        ).toBe('купить офис в Кронштадте');

        expect(
            makeDirectText({
                commercialType: [ 'WAREHOUSE', 'MANUFACTURING' ],
                type: 'SELL',
                category: 'COMMERCIAL'
            }, 'в Кронштадте')
        ).toBe('купить склад производственное помещение в Кронштадте');

        expect(
            makeDirectText({
                type: 'RENT',
                category: 'COMMERCIAL',
                rentTime: 'LARGE'
            }, 'в Кронштадте')
        ).toBe('снять коммерческую недвижимость в аренду в Кронштадте');

        expect(
            makeDirectText({
                commercialType: 'OFFICE',
                type: 'RENT',
                category: 'COMMERCIAL',
                rentTime: 'LARGE'
            }, 'в Кронштадте')
        ).toBe('снять офис в аренду в Кронштадте');

        expect(
            makeDirectText({
                commercialType: [ 'WAREHOUSE', 'MANUFACTURING' ],
                type: 'RENT',
                category: 'COMMERCIAL',
                rentTime: 'LARGE'
            }, 'в Кронштадте')
        ).toBe('снять склад производственное помещение в аренду в Кронштадте');

        // invalid:
        expect(
            makeDirectText({
                type: 'SELL',
                category: 'LOT',
                rentTime: 'SHORT'
            }, 'в Москве')
        ).toBe('купить участок в Москве');

        // invalid:
        expect(
            makeDirectText({
                type: 'SELL',
                category: 'LOT',
                rentTime: 'LARGE'
            }, 'в Москве')
        ).toBe('купить участок в Москве');

        // invalid:
        expect(
            makeDirectText({
                type: 'RENT',
                category: 'GARAGE',
                rentTime: 'SHORT'
            }, 'в Москве')
        ).toBe('снять гараж посуточно в Москве');

        // invalid:
        expect(
            makeDirectText({
                type: 'RENT',
                category: 'COMMERCIAL',
                rentTime: 'SHORT'
            }, 'в Москве')
        ).toBe('снять коммерческую недвижимость в аренду посуточно в Москве');

        // invalid:
        expect(
            makeDirectText({
                commercialType: 'OFFICE',
                type: 'RENT',
                category: 'COMMERCIAL',
                rentTime: 'SHORT'
            }, 'в Москве')
        ).toBe('снять офис в аренду посуточно в Москве');

        // invalid:
        expect(
            makeDirectText({
                commercialType: [ 'WAREHOUSE', 'MANUFACTURING' ],
                type: 'RENT',
                category: 'COMMERCIAL',
                rentTime: 'SHORT'
            }, 'в Москве')
        ).toBe('снять склад производственное помещение в аренду посуточно в Москве');
    });

    it('should replace region suffix', function() {
        expect(
            makeDirectText({
                category: 'COMMERCIAL',
                commercialType: 'OFFICE',
                garageType: 'BOX',
                type: 'SELL'
            },
            'в Москве',
            {
                alwaysShowCommercial: true,
                rgid: 741964
            })
        ).toBe('купить коммерческую недвижимость офис в Москве и Московской области');

        expect(
            makeDirectText({
                category: 'APARTMENT',
                roomsTotal: '3',
                type: 'SELL'
            },
            'sad',
            {
                hideRoomsTotal: true,
                rgid: 741965
            })
        ).toBe('купить квартиру в Санкт-Петербурге и Ленинградской области');
    });
});
