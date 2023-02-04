import type { GetVehicleInfoResponse } from 'auto-core/types/proto/auto/api/vin/garage/response_model';

import getComplectationValueFromVehicleInfo from './getComplectationValueFromVehicleInfo';

describe('getComplectationValueFromVehicleInfo', () => {
    it('возвращает валидное значение, если есть отчет с данными из car_info', () => {
        const vehicleInfo = createVehicleInfo({
            configurationId: '1',
            complectationId: '2',
            techId: '3',
        });

        expect(getComplectationValueFromVehicleInfo(vehicleInfo)).toBe('1_2_3');
    });

    it('возвращает пустую строку, если нет отчета', () => {
        expect(getComplectationValueFromVehicleInfo({})).toBe('');
    });

    it('возвращает пустую строку, если хоть один id - это пустая строка', () => {
        const vehicleInfo = createVehicleInfo({
            configurationId: '',
            complectationId: '2',
            techId: '3',
        });

        expect(getComplectationValueFromVehicleInfo(vehicleInfo)).toBe('');
    });
});

interface Args {
    configurationId: string;
    complectationId: string;
    techId: string;
}

function createVehicleInfo({ complectationId, configurationId, techId }: Args) {
    return {
        report: {
            car_info: {
                configuration_id: configurationId,
                complectation_id: complectationId,
                tech_param_id: techId,
            },
        },
    } as GetVehicleInfoResponse;
}
