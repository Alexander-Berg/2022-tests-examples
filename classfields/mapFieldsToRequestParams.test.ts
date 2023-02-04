import { BuyoutFlow } from 'auto-core/server/blocks/c2bAuction/types';

import mapFieldsToRequestParams from './mapFieldsToRequestParams';

describe('mapFieldsToRequestParams переводит значения полей в параметры запроса', () => {
    it('возвращает правильные параметры запроса, если переданы все поля', () => {
        const mockFields = {
            vin: { value: 'FAKEVIN' },
            license_plate: { value: '777' },
            registration_region_name: { value: 'Moscow' },
            mark: { value: 'Volga' },
            model: { value: 'Novaya' },
            super_gen: { value: 123 },
            body_type: { value: 'ALLROAD_5_DOORS' },
            engine_type: { value: 'GASOLINE' },
            gear_type: { value: 'FORWARD_CONTROL' },
            transmission_full: { value: 'ROBOT' },
            tech_param: { value: 321 },
            complectation: { value: '23' },
            steering_wheel: { value: 'left' },
            color: { value: 'red' },
            year: { value: 1645 },
            run: { value: 20000000 },
            owners_number: { value: '1' },
            pts_info: { value: 'original' },
            price: { value: '10000000' },

            name: { value: 'Petya' },
            phone: { value: '911' },

            inspect_place: { value: 'Москва' },
            inspect_dates: { value: '12.12.2022,13.12.2022' },
            inspect_time: { value: '13:00 - 15:00' },
            inspect_comment: { value: 'Тачка - во!' },

            amocrm_req_id: { value: '1' },
        };

        expect(mapFieldsToRequestParams({ formFields: mockFields, userId: '1', buyoutFlow: BuyoutFlow.AUCTION })).toEqual({
            car_info: {
                vin: 'FAKEVIN',
                license_plate: '777',
                mark: 'Volga',
                model: 'Novaya',
                body_type: 'ALLROAD_5_DOORS',
                engine_type: 'GASOLINE',
                gear_type: 'FORWARD_CONTROL',
                complectation: '23',
                super_gen_id: 123,
                tech_param_id: 321,
                transmission: 'ROBOT',
                steering_wheel: 'left',
                color: 'red',
                year: 1645,
                mileage: 20000000,
                owners_number: '1',
                price: '10000000',
                registration_region: 'Moscow',
                pts_info: 'original',
                seller_info: {
                    name: 'Petya',
                    phone: '911',
                },
                deal_info: {
                    account_manager_id: '1',
                    amocrm_req_id: 1,
                },
            },
            inspect_place: {
                address: 'Москва',
                comment: 'Тачка - во!',
            },
            inspect_time: '13:00 - 15:00',
            inspect_dates: [
                '12.12.2022',
                '13.12.2022',
            ],
            category: 'cars',
            application_source: 'ACCOUNT',
            buy_out_alg: 'AUCTION',
        });
    });

    it('возвращает пустой объект пользователя, если нет информации о телефоне и имени', () => {
        const mockFields = {
            vin: { value: 'FAKEVIN' },
            license_plate: { value: '777' },
            registration_region_name: { value: 'Moscow' },
            mark: { value: 'Volga' },
            model: { value: 'Novaya' },
            super_gen: { value: 123 },
            body_type: { value: 'ALLROAD_5_DOORS' },
            engine_type: { value: 'GASOLINE' },
            gear_type: { value: 'FORWARD_CONTROL' },
            transmission_full: { value: 'ROBOT' },
            tech_param: { value: 321 },
            complectation: { value: 'complectation' },
            steering_wheel: { value: 'left' },
            color: { value: 'red' },
            year: { value: 1645 },
            run: { value: 20000000 },
            owners_number: { value: '1' },
            pts_info: { value: 'original' },
            price: { value: '10000000' },

            inspect_place: { value: 'Москва' },
            inspect_dates: { value: '12.12.2022,13.12.2022' },
            inspect_time: { value: '13:00 - 15:00' },
            inspect_comment: { value: 'Тачка - во!' },

            amocrm_req_id: { value: '1' },
        };

        expect(mapFieldsToRequestParams({
            formFields: mockFields,
            userId: '1',
            buyoutFlow: BuyoutFlow.WITH_PRE_OFFERS,
        }).car_info.seller_info).toEqual({});
    });

    it('возвращает объект с информацией о сделке только с полем id АМа со значением undefined, если нет id запроса в амо и id пользователя', () => {
        const mockFields = {
            vin: { value: 'FAKEVIN' },
            license_plate: { value: '777' },
            registration_region_name: { value: 'Moscow' },
            mark: { value: 'Volga' },
            model: { value: 'Novaya' },
            super_gen: { value: 123 },
            body_type: { value: 'ALLROAD_5_DOORS' },
            engine_type: { value: 'GASOLINE' },
            gear_type: { value: 'FORWARD_CONTROL' },
            transmission_full: { value: 'ROBOT' },
            tech_param: { value: 321 },
            complectation: { value: 'complectation' },
            steering_wheel: { value: 'left' },
            color: { value: 'red' },
            year: { value: 1645 },
            run: { value: 20000000 },
            owners_number: { value: '1' },
            pts_info: { value: 'original' },
            price: { value: '10000000' },

            inspect_place: { value: 'Москва' },
            inspect_dates: { value: '12.12.2022,13.12.2022' },
            inspect_time: { value: '13:00 - 15:00' },
            inspect_comment: { value: 'Тачка - во!' },
        };

        expect(mapFieldsToRequestParams({ formFields: mockFields, buyoutFlow: BuyoutFlow.AUCTION }).car_info.deal_info).toEqual({
            account_manager_id: undefined,
        });
    });

    it('возвращает пустой объект с местом осмотра, если нет места осмотра и комментария', () => {
        const mockFields = {
            vin: { value: 'FAKEVIN' },
            license_plate: { value: '777' },
            registration_region_name: { value: 'Moscow' },
            mark: { value: 'Volga' },
            model: { value: 'Novaya' },
            super_gen: { value: 123 },
            body_type: { value: 'ALLROAD_5_DOORS' },
            engine_type: { value: 'GASOLINE' },
            gear_type: { value: 'FORWARD_CONTROL' },
            transmission_full: { value: 'ROBOT' },
            tech_param: { value: 321 },
            complectation: { value: 'complectation' },
            steering_wheel: { value: 'left' },
            color: { value: 'red' },
            year: { value: 1645 },
            run: { value: 20000000 },
            owners_number: { value: '1' },
            pts_info: { value: 'original' },
            price: { value: '10000000' },

            inspect_dates: { value: '12.12.2022,13.12.2022' },
            inspect_time: { value: '13:00 - 15:00' },
        };

        expect(mapFieldsToRequestParams({ formFields: mockFields, buyoutFlow: BuyoutFlow.AUCTION }).inspect_place).toEqual({});
    });

    it('не возвращает поля с датой и временем осмотра, если нет даты и осмотра', () => {
        const mockFields = {
            vin: { value: 'FAKEVIN' },
            license_plate: { value: '777' },
            registration_region_name: { value: 'Moscow' },
            mark: { value: 'Volga' },
            model: { value: 'Novaya' },
            super_gen: { value: 123 },
            body_type: { value: 'ALLROAD_5_DOORS' },
            engine_type: { value: 'GASOLINE' },
            gear_type: { value: 'FORWARD_CONTROL' },
            transmission_full: { value: 'ROBOT' },
            tech_param: { value: 321 },
            complectation: { value: '23' },
            steering_wheel: { value: 'left' },
            color: { value: 'red' },
            year: { value: 1645 },
            run: { value: 20000000 },
            owners_number: { value: '1' },
            pts_info: { value: 'original' },
            price: { value: '10000000' },
        };

        expect(mapFieldsToRequestParams({ formFields: mockFields, buyoutFlow: BuyoutFlow.WITH_PRE_OFFERS })).toEqual({
            car_info: {
                vin: 'FAKEVIN',
                license_plate: '777',
                mark: 'Volga',
                model: 'Novaya',
                body_type: 'ALLROAD_5_DOORS',
                engine_type: 'GASOLINE',
                gear_type: 'FORWARD_CONTROL',
                complectation: '23',
                super_gen_id: 123,
                tech_param_id: 321,
                transmission: 'ROBOT',
                steering_wheel: 'left',
                color: 'red',
                year: 1645,
                mileage: 20000000,
                owners_number: '1',
                price: '10000000',
                registration_region: 'Moscow',
                pts_info: 'original',
                seller_info: {},
                deal_info: {
                    account_manager_id: undefined,
                },
            },
            inspect_place: {},
            category: 'cars',
            application_source: 'ACCOUNT',
            buy_out_alg: 'WITH_PRE_OFFERS',
        });
    });

    it('возвращает минимальный набор полей, если передан пустой объект', () => {
        expect(mapFieldsToRequestParams({ formFields: {}, buyoutFlow: BuyoutFlow.WITH_PRE_OFFERS })).toEqual({
            car_info: {
                deal_info: {
                    account_manager_id: undefined,
                },
                seller_info: {},
            },
            inspect_place: {},
            category: 'cars',
            application_source: 'ACCOUNT',
            buy_out_alg: 'WITH_PRE_OFFERS',
        });
    });
});
