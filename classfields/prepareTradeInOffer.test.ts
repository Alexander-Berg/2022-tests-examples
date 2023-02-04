import prepareTradeInOffer from './prepareTradeInOffer';

describe('prepareTradeInOffer', () => {
    it('формирует оффер для ручки трейд-ина', () => {
        const mockParams = {
            mark: 'AUDI',
            model: 'A3',
            year: '2018',
            body_type: 'SEDAN',
            super_gen: '20785010',
            engine_type: 'GASOLINE',
            gear_type: 'FORWARD_CONTROL',
            transmission_full: 'ROBOT',
            tech_param: '20786016',
            tech_param_id: '20786016',
            equipment: [],
            run: 12,
            km_age: 12,
            color: 'EE1D19',
            rid: '62',
            address: {
                geoId: '62',
                cityName: 'Красноярск',
                address: null,
                coordinates: [ 56.010563, 92.852572 ],
                geoParentsIds: [ 62, 121043, 11309, 59, 225, 10001, 10000 ],
            },
            owners_count: 1,
            canCreateRedirect: false,
            redirectPhones: true,
            notDisturb: false,
            chatsDisabled: false,
        };

        expect(prepareTradeInOffer(mockParams)).toEqual({
            availability: 'IN_STOCK',
            car_info: {
                body_type: 'SEDAN',
                drive: 'FORWARD_CONTROL',
                equipment: { gbo: false },
                engine_type: 'GASOLINE',
                mark: 'AUDI',
                model: 'A3',
                super_gen_id: '20785010',
                tech_param_id: '20786016',
                transmission: 'ROBOT',
                steering_wheel: 'LEFT',
            },
            category: 'CARS',
            color_hex: 'EE1D19',
            section: 'USED',
            description: undefined,
            discount_options: { tradein: 0, insurance: 0, credit: 0, max_discount: 0 },
            documents: {
                custom_cleared: true,
                license_plate: undefined,
                owners_number: '1',
                sts: undefined,
                vin: undefined,
                year: '2018',
            },
            state: {
                condition: 'CONDITION_OK',
                state_not_beaten: true,
                mileage: 12,
                image_urls: [],
                disable_photo_reorder: false,
                hide_license_plate: undefined,
                interior_panorama: { panoramas: [] },
            },
            additional_info: {
                exchange: false,
                trusted_dealer_calls_accepted: false,
                hidden: false,
                chat_only: false,
                online_view_available: false,
                accepted_autoru_exclusive: false,
            },
            seller: {
                location: { geobase_id: '62' },
            },
            options_updated: undefined,
            trade_in_info: {
                trade_in_type: 'NONE',
                trade_in_price_info: { price: 0, currency: 'RUR' },
            },
            is_safe_deal_disabled: true,
        });
    });
});
