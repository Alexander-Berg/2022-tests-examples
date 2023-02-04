import { enrichBuildingInfo, offerDataToPreview } from '../transformer';

describe('enrichBuildingInfo', () => {
    it('not enrich field if field is not nil in VOS-model', () => {
        const vosCard = {
            specific: {
                category: 'ROOMS',
                buildingType: 'PANEL'
            },
            offer: {}
        };

        const buildingInfo = {
            buildingType: 'PANEL',
            buildYear: '2000'
        };

        const enriched = enrichBuildingInfo(vosCard, buildingInfo);

        expect(enriched.specific.buildingType).toEqual('PANEL');
        expect(enriched.specific.builtYear).toEqual('2000');

        expect(enriched.specific.enrichedFields).toEqual([
            'BUILD_YEAR'
        ]);
    });

    it('enrich schools for non-commercial offer', () => {
        const vosCard = {
            specific: {
                category: 'LOT'
            },
            offer: {}
        };

        const buildingInfo = {
            schools: [ {
                schoolId: 123
            } ]
        };

        const enriched = enrichBuildingInfo(vosCard, buildingInfo);

        expect(enriched.specific.schools).toEqual([ {
            schoolId: 123
        } ]);
    });

    it('not enrich schools for commercial offer', () => {
        const vosCard = {
            specific: {
                category: 'COMMERCIAL'
            },
            offer: {}
        };

        const buildingInfo = {
            schools: [ {
                schoolId: 123
            } ]
        };

        const enriched = enrichBuildingInfo(vosCard, buildingInfo);

        expect(enriched.specific.schools).toBeUndefined();
    });

    it('enrich correct searcher-models fields offerDataToPreview', () => {
        const vosCard = {
            specific: {
                category: 'ROOMS'
            },
            offer: {},
            user: {}
        };

        const buildingInfo = {
            buildingId: 12345,
            buildingType: 'PANEL',
            buildYear: '2000',
            buildingSeriesId: '1111',
            buildingSeries: 'П-222',
            floors: 3,
            flatsCount: 33,
            hasElevator: true,
            hasRubbishChute: true,
            porchesCount: 2,
            hasSecurity: true,
            ceilingHeight: 200,
            hasGas: true,
            hasWater: true,
            heatingType: 'CENTRAL',
            hasSewerage: true,
            hasElectricity: true,
            isGuarded: true,
            priceStatistics: {
                profitability: {
                    level: 5,
                    maxLevel: 9,
                    value: '19'
                },
                sellPricePerSquareMeter: {
                    level: 5,
                    maxLevel: 9,
                    value: '205411'
                }
            }
        };

        const searcherModel = offerDataToPreview({ vosCard, buildingInfo });

        expect(searcherModel.building.buildingType).toEqual('PANEL');
        expect(searcherModel.building.buildingId).toEqual(12345);
        expect(searcherModel.building.builtYear).toEqual('2000');
        expect(searcherModel.building.buildingSeriesId).toEqual('1111');
        expect(searcherModel.building.buildingSeries).toEqual('П-222');
        expect(searcherModel.floorsTotal).toEqual(3);
        expect(searcherModel.building.flatsCount).toEqual(33);
        expect(searcherModel.building.improvements.LIFT).toEqual(true);
        expect(searcherModel.building.improvements.RUBBISH_CHUTE).toEqual(true);
        expect(searcherModel.building.porchesCount).toEqual(2);
        expect(searcherModel.building.improvements.SECURITY).toEqual(true);
        expect(searcherModel.building.improvements.GUARDED).toEqual(true);
        expect(searcherModel.ceilingHeight).toEqual(2);
        expect(searcherModel.supplyMap.GAS).toEqual(true);
        expect(searcherModel.supplyMap.WATER).toEqual(true);
        expect(searcherModel.supplyMap.HEATING).toEqual('CENTRAL');
        expect(searcherModel.building.heatingType).toEqual('CENTRAL');
        expect(searcherModel.supplyMap.SEWERAGE).toEqual(true);
        expect(searcherModel.supplyMap.ELECTRICITY).toEqual(true);
        expect(searcherModel.building.priceStatistics.profitability.value).toEqual('19');
        expect(searcherModel.building.priceStatistics.sellPricePerSquareMeter.value).toEqual('205411');

        expect(searcherModel.enrichedFields).toEqual([
            'PRICE_STATISTICS',
            'BUILDING_TYPE',
            'BUILDING_ID',
            'BUILD_YEAR',
            'BUILDING_SERIES_ID',
            'BUILDING_SERIES_ID',
            'FLOORS_COUNT',
            'FLATS_COUNT',
            'LIFT',
            'RUBBISH_CHUTE',
            'PORCHES_COUNT',
            'SECURITY',
            'GUARDED',
            'CEILING_HEIGHT',
            'GAS',
            'WATER',
            'HEATING_TYPE',
            'SEWERAGE',
            'ELECTRICITY'
        ]);
    });

    it('enrich schools in correct searcher-models field', () => {
        const vosCard = {
            specific: {},
            offer: {},
            user: {}
        };

        const buildingInfo = {
            schools: [ { id: '123' } ]
        };

        const searcherModel = offerDataToPreview({ vosCard, buildingInfo });

        expect(searcherModel.location.schools).toEqual([ { id: '123' } ]);
    });

    it('no enrich data for newbuilding', () => {
        const vosCard = {
            specific: {
                category: 'ROOMS',
                siteId: 'some site id'
            },
            offer: {},
            user: {}
        };

        const buildingInfo = {
            buildingId: 'some id'
        };

        const searcherModel = offerDataToPreview({ vosCard, buildingInfo });

        expect(searcherModel.enrichedFields).toBeUndefined();
        expect(searcherModel.building.buildingId).toBeUndefined();
    });

    it('enrich data only for aparatments and rooms', () => {
        const vosCard = {
            specific: {
                category: 'LOT'
            },
            offer: {},
            user: {}
        };

        const buildingInfo = {
            buildingId: 'some id'
        };

        const searcherModel = offerDataToPreview({ vosCard, buildingInfo });

        expect(searcherModel.enrichedFields).toBeUndefined();
        expect(searcherModel.building.buildingId).toBeUndefined();
    });

    it('enrich expected metros', () => {
        const vosCard = {
            specific: {},
            offer: {},
            user: {}
        };

        const buildingInfo = {
            expectedMetros: [ {
                metroId: 744463,
                name: 'Зюзино',
                latitude: 55.65676498413086,
                longitude: 37.57178497314453,
                lineName: 'Большая кольцевая линия',
                lineId: 23,
                year: 2019,
                rgbColor: '7f0000',
                time: 294,
                type: 'ON_FOOT',
                description:
                    'Выходы на обе стороны Севастопольского проспекта и улицы Каховка, ' +
                    'к жилой и общественной застройке, остановкам наземного пассажирского транспорта. ' +
                    'Количество вестибюлей два подземных.'
            } ]
        };

        const searcherModel = offerDataToPreview({ vosCard, buildingInfo });

        expect(searcherModel.location.expectedMetroList).toHaveLength(1);
        expect(searcherModel.location.expectedMetroList[0]).toEqual({
            metroId: 744463,
            metroTransport: 'ON_FOOT',
            minTimeToMetro: 4,
            name: 'Зюзино',
            rgbColor: '7f0000',
            timeToMetro: 4,
            year: 2019
        });
    });
});
