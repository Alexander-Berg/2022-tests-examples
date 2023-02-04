import type { EnrichedGetEvaluationParams } from '../types';

import mapEvaluationShareToC2BCarInfo from './mapEvaluationShareToC2BCarInfo';

describe('mapEvaluationShareToC2BCarInfo', () => {
    it('правильно мапит валидные данные', () => {
        const mockParams = {
            year: '2012',
            tech_param: '212111',
            configuration_id: '121211',
            steering_wheel: 'RIGHT',
            run: 12,
            address: {
                geoId: 1,
                coordinates: [ 55, 33 ],
            },
            rid: 213,
        } as unknown as EnrichedGetEvaluationParams;

        expect(mapEvaluationShareToC2BCarInfo(mockParams)).toEqual({
            year: 2012,
            configuration_id: 121211,
            tech_param_id: 212111,
            steering_wheel: 'RIGHT',
            mileage: 12,
            location: {
                geobase_id: 213,
                geo_point: {
                    latitude: 55,
                    longitude: 33,
                },
            },
        });
    });

    it('возвращает руль по-умолчанию, если руль не заполнен', () => {
        const mockParams = {
            year: '2012',
            configuration_id: '121211',
            tech_param: '212111',
            run: 12,
            address: {
                geoId: 1,
                coordinates: [ 55, 33 ],
            },
        } as unknown as EnrichedGetEvaluationParams;

        expect(mapEvaluationShareToC2BCarInfo(mockParams).steering_wheel).toBe('LEFT');
    });

    it('возвращает undefined вместо широты и долготы, если нет координат', () => {
        const mockParams = {
            year: '2012',
            configuration_id: '121211',
            tech_param: '212111',
            run: 12,
            address: {
                geoId: 1,
                coordinates: [],
            },
        } as unknown as EnrichedGetEvaluationParams;

        expect(mapEvaluationShareToC2BCarInfo(mockParams).location?.geo_point).toEqual({
            latitude: undefined,
            longitude: undefined,
        });
    });

    it('возвращает undefined вместо id геобазы, если такого id нет', () => {
        const mockParams = {
            year: '2012',
            configuration_id: '121211',
            tech_param: '212111',
            run: 12,
            address: {
                coordinates: [ 55, 33 ],
            },
        } as unknown as EnrichedGetEvaluationParams;

        expect(mapEvaluationShareToC2BCarInfo(mockParams).location?.geobase_id).toBeUndefined();
    });

    it('возвращает undefined вместо адреса, если адрес не был передан', () => {
        const mockParams = {
            year: '2012',
            configuration_id: '121211',
            tech_param: '212111',
            run: 12,
        } as unknown as EnrichedGetEvaluationParams;

        expect(mapEvaluationShareToC2BCarInfo(mockParams).location).toBeUndefined();
    });
});
