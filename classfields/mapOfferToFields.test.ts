import type { Offer as RawOffer } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';

import userOfferResponse from 'autoru-frontend/mockData/responses/userOffer.mock';

import mapOfferToFields from './mapOfferToFields';

describe('mapOfferToFields переводит поля оффера в поля формы', () => {
    it('возвращает объект с полем carInfo и generalInfo', () => {
        const mockOffer = userOfferResponse.result.offer as unknown as RawOffer;

        expect(mapOfferToFields(mockOffer)).toEqual({
            carInfo: [
                {
                    name: 'mark',
                    value: 'FORD',
                },
                {
                    name: 'model',
                    value: 'ECOSPORT',
                },
                {
                    name: 'year',
                    value: '2017',
                },
                {
                    name: 'super_gen',
                    value: '20104320',
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
                    value: '20104325',
                },
                {
                    name: 'complectation',
                    value: '',
                },
            ],
            generalInfo: {
                color: { value: '040001' },
                license_plate: { value: 'р322сн58' },
                name: { value: 'Дмитрий' },
                owners_count: { value: 1 },
                phone: { value: '79250166346' },
                price: { value: 855000 },
                pts_info: { value: 'ORIGINAL' },
                run: { value: 18500 },
                steering_wheel: { value: 'LEFT' },
                vin: { value: 'Z6FLXXECHLHJ26528' },
            },
        });
    });

    it('возвращает объект с полями carInfo и generalInfo, где значения пустые строки, если offer undefined', () => {
        expect(mapOfferToFields(undefined)).toEqual({
            carInfo: [
                {
                    name: 'mark',
                    value: '',
                },
                {
                    name: 'model',
                    value: '',
                },
                {
                    name: 'year',
                    value: '',
                },
                {
                    name: 'super_gen',
                    value: '',
                },
                {
                    name: 'body_type',
                    value: '',
                },
                {
                    name: 'engine_type',
                    value: '',
                },
                {
                    name: 'gear_type',
                    value: '',
                },
                {
                    name: 'transmission_full',
                    value: '',
                },
                {
                    name: 'tech_param',
                    value: '',
                },
                {
                    name: 'complectation',
                    value: '',
                },
            ],
            generalInfo: {
                color: { value: '' },
                license_plate: { value: '' },
                name: { value: '' },
                owners_count: { value: '' },
                phone: { value: '' },
                price: { value: '' },
                pts_info: { value: '' },
                run: { value: '' },
                steering_wheel: { value: '' },
                vin: { value: '' },
            },
        });
    });
});
