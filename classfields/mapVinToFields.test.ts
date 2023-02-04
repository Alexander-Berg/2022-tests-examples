import type { RawVinReport } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';

import myReportsMock from 'auto-core/react/dataDomain/myReports/mocks/myReports.mock';

import mapVinToFields from './mapVinToFields';

describe('mapVinToFields переводит информация о тачке по ВИНу в поля формы', () => {
    it('возвращает полные данные от тачки', () => {
        const mockReport = {
            car_info: {
                mark: 'Volga',
                model: 'Novaya',
                super_gen_id: '10',
                body_type: 'ALLROAD_5_DOORS',
                engine_type: 'GASOLINE',
                drive: 'FORWARD_CONTROL',
                transmission: 'ROBOT',
                configuration_id: '1',
                complectation_id: '2',
                tech_param_id: '3',
            },
            vin: 'FAKEVIN',
            pts_info: {
                licence_plate: {
                    value_text: 'ф228уе',
                },
                year: {
                    value: 2004,
                },
            },
            pts_owners: {
                record_count: 1,
            },
            vehicle: {
                color_hex: 'FFFFFF',
            },
        } as unknown as RawVinReport;

        expect(mapVinToFields({ report: mockReport })).toEqual({
            carInfo: [
                {
                    name: 'mark',
                    value: 'Volga',
                },
                {
                    name: 'model',
                    value: 'Novaya',
                },
                {
                    name: 'year',
                    value: '2004',
                },
                {
                    name: 'super_gen',
                    value: '10',
                },
                {
                    name: 'body_type',
                    value: 'ALLROAD_5_DOORS',
                },
                {
                    name: 'engine_type',
                    value: 'GASOLINE',
                },
                {
                    name: 'gear_type',
                    value: 'FORWARD_CONTROL',
                },
                {
                    name: 'transmission_full',
                    value: 'ROBOT',
                },
                {
                    name: 'tech_param',
                    value: '3',
                },
                {
                    name: 'complectation',
                    value: '1_2_3',
                },
            ],
            generalInfo: {
                color: { value: 'FFFFFF' },
                license_plate: { value: 'ф228уе' },
                owners_count: { value: 1 },
                vin: { value: 'FAKEVIN' },
            },
        });
    });

    it('останавливает заполнение массива carInfo, если нет данных о тачке', () => {
        expect(mapVinToFields({ report: myReportsMock.reports[0] })).toEqual({
            carInfo: [
                {
                    name: 'mark',
                    value: 'Nissan',
                },
                {
                    name: 'model',
                    value: 'Almera',
                },
                {
                    name: 'year',
                    value: '2015',
                },
            ],
            generalInfo: {
                color: { value: '' },
                license_plate: { value: '' },
                owners_count: { value: '' },
                vin: { value: 'Z0NZWE00054341234' },
            },
        });
    });

    it('возвращает пустой объект, если нет отчета', () => {
        expect(mapVinToFields({})).toEqual({});
    });
});
