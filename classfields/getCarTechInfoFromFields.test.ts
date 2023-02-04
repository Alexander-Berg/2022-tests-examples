import { disk, nbsp } from 'auto-core/react/lib/html-entities';

import { getCarTechInfoFromFields, getMileage } from './getCarTechInfoFromFields';

describe('getCarTechInfoFromFields - функция-маппер для полей из car_info', () => {
    it('возвращает полную информацию о машине, если переданы все данные', () => {
        const mockCarInfo = {
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
        expect(getCarTechInfoFromFields(mockCarInfo)).toBe(
            `10${ nbsp }000${ nbsp }км${ nbsp }${ disk } Задний${ nbsp }${ disk } Бензин${ nbsp }${ disk } Седан`,
        );
    });

    it('возвращает пробег, если переданы только обязательные поля', () => {
        const mockCarInfo = {
            mark: 'Audi',
            model: 'A3',
            year: 2017,
            mileage: 10000,
            seller_info: {
                phone: '95444444444',
            },
        };
        expect(getCarTechInfoFromFields(mockCarInfo)).toBe(`10${ nbsp }000${ nbsp }км`);
    });
});

describe('getMileage - функция получения форматированного пробега из car_info', () => {
    it('возвращает пробег с км, если пробег больше 0', () => {
        expect(getMileage(1)).toBe(`1${ nbsp }км`);
    });

    it('возвращает "новый", если пробег равен 0', () => {
        expect(getMileage(0)).toBe('новый');
    });
});
