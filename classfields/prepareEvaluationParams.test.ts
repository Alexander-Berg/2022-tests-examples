import createHttpReq from 'autoru-frontend/mocks/createHttpReq';
import createHttpRes from 'autoru-frontend/mocks/createHttpRes';

import createContext from 'auto-core/server/descript/createContext';

import type { THttpRequest } from 'auto-core/http';

import type { GetEvaluationParams } from '../types';

import prepareEvaluationParams from './prepareEvaluationParams';

describe('prepareEvaluationParams', () => {
    it('добавляет параметры rid, km_age, tech_param_id', () => {
        const mockContext = createMockContext();
        const mockParams = {
            run: 1,
            address: {
                geo_id: '1',
            },
            tech_param: '777',
        } as GetEvaluationParams;

        expect(prepareEvaluationParams({
            context: mockContext,
            params: mockParams,
        })).toEqual({
            run: 1,
            km_age: 1,
            address: {
                geo_id: '1',
            },
            rid: '1',
            tech_param: '777',
            tech_param_id: '777',
        });
    });

    it('берет rid из geoId, если нет geo_id', () => {
        const mockContext = createMockContext();
        const mockParams = {
            run: 1,
            address: {
                geoId: '1',
            },
            tech_param: '777',
        } as GetEvaluationParams;

        expect(prepareEvaluationParams({
            context: mockContext,
            params: mockParams,
        })).toEqual({
            run: 1,
            km_age: 1,
            address: {
                geoId: '1',
            },
            rid: '1',
            tech_param: '777',
            tech_param_id: '777',
        });
    });

    it('берет rid из региона пользователя, если нет данных в параметрах', () => {
        const mockContext = createMockContext({ geoIds: [ '2' ] });
        const mockParams = {
            run: 1,
            tech_param: '777',
        } as GetEvaluationParams;

        expect(prepareEvaluationParams({
            context: mockContext,
            params: mockParams,
        })).toEqual({
            run: 1,
            km_age: 1,
            rid: '2',
            tech_param: '777',
            tech_param_id: '777',
        });
    });

    it('берет rid из региона по IP, если нет данных в параметрах и регионе запроса', () => {
        const mockContext = createMockContext({ regionByIp: { id: '3' } });
        const mockParams = {
            run: 1,
            tech_param: '777',
        } as GetEvaluationParams;

        expect(prepareEvaluationParams({
            context: mockContext,
            params: mockParams,
        })).toEqual({
            run: 1,
            km_age: 1,
            rid: '3',
            tech_param: '777',
            tech_param_id: '777',
        });
    });

    it('возвращает undefined в дополнительных параметров, если передан пустой объект', () => {
        const mockContext = createMockContext();
        const mockParams = {} as GetEvaluationParams;

        expect(prepareEvaluationParams({
            context: mockContext,
            params: mockParams,
        })).toEqual({
            km_age: undefined,
            rid: undefined,
            tech_param_id: undefined,
        });
    });
});

function createMockContext(reqParams?: Record<string, unknown>) {
    return createContext({
        req: {
            ...createHttpReq(),
            ...(reqParams ? reqParams : undefined),
        } as THttpRequest,
        res: createHttpRes(),
    });
}
