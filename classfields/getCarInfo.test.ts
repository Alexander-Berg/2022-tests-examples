import type { Offer } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';
import { OfferStatus } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';

import * as getImageUrlsModule from 'auto-core/react/lib/offer/getImageUrls';

import * as getCarTechInfoFromOfferModule from './getCarTechInfoFromOffer';
import * as getCarNameFromOfferModule from './getCarNameFromOffer';
import * as getCarTechInfoFromFieldsModule from './getCarTechInfoFromFields';
import * as getCarNameFromFieldsModule from './getCarNameFromFields';
import { getCarInfo } from './getCarInfo';

describe('getCarInfo - функция, которая возвращает информацию о машине в зависимости от типа аргумента', () => {
    it('вызывает функции-мапперы для оффера, если передан оффер', () => {
        const getCarTechInfoFromOfferMock = jest.spyOn(getCarTechInfoFromOfferModule, 'getCarTechInfoFromOffer');
        const getImageUrlsMock = jest.spyOn(getImageUrlsModule, 'default');
        const getCarNameFromOfferMock = jest.spyOn(getCarNameFromOfferModule, 'getCarNameFromOffer');

        const mockData = createMockOffer();

        getCarInfo(mockData);

        expect(getCarTechInfoFromOfferMock).toHaveBeenCalledTimes(1);
        expect(getImageUrlsMock).toHaveBeenCalledTimes(1);
        expect(getCarNameFromOfferMock).toHaveBeenCalledTimes(1);
    });

    it('вызывает функции-мапперы car_info, если передано только поле car_info', () => {
        const getCarTechInfoFromFieldsMock = jest.spyOn(getCarTechInfoFromFieldsModule, 'getCarTechInfoFromFields');
        const getCarNameFromFieldsMock = jest.spyOn(getCarNameFromFieldsModule, 'getCarNameFromFields');

        const mockData = createMockCarInfo();

        getCarInfo(mockData);

        expect(getCarTechInfoFromFieldsMock).toHaveBeenCalledTimes(1);
        expect(getCarNameFromFieldsMock).toHaveBeenCalledTimes(1);
    });

    it('возвращает объект с пустыми значениями, если нет ни оффера, ни car_info', () => {
        expect(getCarInfo(undefined)).toEqual({
            carDescription: '',
            carName: '',
            carImageUrl: '',
        });
    });
});

function createMockOffer() {
    return {
        car_info: {
            body_type: 'SEDAN',
            drive: 'FORWARD_CONTROL',
            engine_type: 'GASOLINE',
            horse_power: 150,
            mark: 'AUDI',
            mark_info: { code: 'AUDI', name: 'Audi' },
            model: 'A3',
            model_info: { code: 'A3', name: 'A3' },
            steering_wheel: 'LEFT',
            transmission: 'ROBOT',
        },
        state: {
            mileage: 10000,
        },
        documents: {
            vin: 'FAKEVINFAKEVIN',
            year: 2017,
        },
        id: 1,
        status: OfferStatus.ACTIVE,
    } as unknown as Offer;
}

function createMockCarInfo() {
    return {
        vin: 'VIN',
        license_plate: 'госномер',
        registration_region: 'Москва',
        mark: 'Audi',
        model: 'A3',
        year: 2017,
        configuration_id: 1,
        super_gen_id: 2,
        modification_id: 3,
        tech_param_id: 4,
        body_type: 'седан',
        engine_type: 'бензин',
        transmission: 'АМТ',
        gear_type: 'задний',
        color: 'FFFFFF',
        mileage: 10000,
        steering_wheel: 'СЛЕВА',
        is_broken: false,
        is_registered_in_russia: true,
        seller_info: {
            name: 'Gon',
            phone: '95444444444',
            user_id_in_auto_ru: '1111111',
        },
    };
}
