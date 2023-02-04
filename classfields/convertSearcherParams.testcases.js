// тест-кейсы для конвертации параметров searcher<->public_api

module.exports = [
    // нечего конвертировать
    {
        searcher: {
            price_to: '100000',
        },
        public_api: null,
    },
    {
        searcher: null,
        public_api: {
            price_to: '100000',
        },
    },

    // autoru_body_type
    {
        searcher: {
            autoru_body_type: [ 'SEDAN' ],
        },
        public_api: {
            body_type_group: [ 'SEDAN' ],
        },
    },
    {
        searcher: {
            autoru_body_type: [ 'HATCHBACK_LIFTBACK' ],
        },
        public_api: {
            body_type_group: [ 'LIFTBACK' ],
        },
    },
    {
        searcher: {
            autoru_body_type: [ 'SEDAN', 'HATCHBACK_LIFTBACK' ],
        },
        public_api: {
            body_type_group: [ 'SEDAN', 'LIFTBACK' ],
        },
    },
    // beaten
    {
        searcher: {
            beaten: '0',
        },
        public_api: {
            damage_group: 'ANY',
        },
    },
    {
        searcher: {
            beaten: '1',
        },
        public_api: {
            damage_group: 'NOT_BEATEN',
        },
    },
    {
        searcher: {
            beaten: '2',
        },
        public_api: {
            damage_group: 'BEATEN',
        },
    },
    // body_key + agricultural
    {
        category: 'TRUCKS',
        searcher: {
            body_key: 'SEEDER',
            trucks_category: 'agricultural',
        },
        public_api: {
            agricultural_type: 'SEEDER',
            trucks_category: 'AGRICULTURAL',
        },
    },
    // body_key + autoloader
    {
        category: 'TRUCKS',
        searcher: {
            body_key: 'FORKLIFTS_ELECTRO',
            trucks_category: 'autoloader',
        },
        public_api: {
            autoloader_type: 'FORKLIFTS_ELECTRO',
            trucks_category: 'AUTOLOADER',
        },
    },
    // body_key + bulldozer
    {
        category: 'TRUCKS',
        searcher: {
            body_key: 'WHEELS_BULLDOZER',
            trucks_category: 'bulldozers',
        },
        public_api: {
            bulldozer_type: 'WHEELS_BULLDOZER',
            trucks_category: 'BULLDOZERS',
        },
    },
    // body_key + construction
    {
        category: 'TRUCKS',
        searcher: {
            body_key: 'ASPHALT_PAVER',
            trucks_category: 'construction',
        },
        public_api: {
            construction_type: 'ASPHALT_PAVER',
            trucks_category: 'CONSTRUCTION',
        },
    },
    // body_key + dredge
    {
        category: 'TRUCKS',
        searcher: {
            body_key: 'PLANNER_EXCAVATOR',
            trucks_category: 'dredge',
        },
        public_api: {
            dredge_type: 'PLANNER_EXCAVATOR',
            trucks_category: 'DREDGE',
        },
    },
    // body_key + lcv
    {
        category: 'TRUCKS',
        searcher: {
            body_key: 'PICKUP',
            trucks_category: 'lcv',
        },
        public_api: {
            light_truck_type: 'PICKUP',
            trucks_category: 'LCV',
        },
    },
    // body_key + municipal
    {
        category: 'TRUCKS',
        searcher: {
            body_key: 'VACUUM_MACHINE',
            trucks_category: 'municipal',
        },
        public_api: {
            municipal_type: 'VACUUM_MACHINE',
            trucks_category: 'MUNICIPAL',
        },
    },
    // body_key + truck
    {
        category: 'TRUCKS',
        searcher: {
            body_key: 'AUTOTRANSPORTER',
            trucks_category: 'truck',
        },
        public_api: {
            trucks_category: 'TRUCK',
            truck_type: 'AUTOTRANSPORTER',
        },
    },
    // catalog_equipment
    {
        searcher: {
            armored_status: '1',
        },
        public_api: {
            catalog_equipment: [ 'armored' ],
        },
    },
    {
        searcher: {
            armored_status: '1',
            catalog_equipment: [ 'tv' ],
        },
        public_api: {
            catalog_equipment: [ 'tv', 'armored' ],
        },
    },
    // cabin_key
    {
        searcher: {
            cabin_key: [ '2_SEAT_1_SLEEP' ],
        },
        public_api: {
            cabin_key: [ 'SEAT_2_1_SLEEP' ],
        },
    },
    {
        searcher: {
            cabin_key: [ '2_SEAT_1_SLEEP', '2_SEAT_WO_SLEEP' ],
        },
        public_api: {
            cabin_key: [ 'SEAT_2_1_SLEEP', 'SEAT_2_WO_SLEEP' ],
        },
    },
    // customs_state
    {
        searcher: {
            customs_state: 'ALL',
        },
        public_api: {
            customs_state_group: 'DOESNT_MATTER',
        },
    },
    {
        searcher: {
            customs_state: '1',
        },
        public_api: {
            customs_state_group: 'CLEARED',
        },
    },
    {
        searcher: {
            customs_state: '2',
        },
        public_api: {
            customs_state_group: 'NOT_CLEARED',
        },
    },
    // custom_state_key
    {
        category: 'MOTO',
        searcher: {
            custom_state_key: 'ALL',
        },
        public_api: {
            customs_state_group: 'DOESNT_MATTER',
        },
    },
    {
        category: 'TRUCKS',
        searcher: {
            custom_state_key: 'CLEARED',
        },
        public_api: {
            customs_state_group: 'CLEARED',
        },
    },
    // cylinders
    {
        category: 'MOTO',
        searcher: {
            cylinders: [ '2' ],
        },
        public_api: {
            cylinders: [ 'CYLINDERS_2' ],
        },
    },
    {
        category: 'MOTO',
        searcher: {
            cylinders: [ '1', '2' ],
        },
        public_api: {
            cylinders: [ 'CYLINDERS_1', 'CYLINDERS_2' ],
        },
    },
    // dealer_org_type
    {
        searcher: {
            dealer_org_type: '4',
        },
        public_api: {
            seller_group: 'PRIVATE',
        },
    },
    {
        searcher: {
            dealer_org_type: '1_2_3_5',
        },
        public_api: {
            seller_group: 'COMMERCIAL',
        },
    },
    // engine_type
    {
        category: 'CARS',
        searcher: {
            engine_type: [ 'GASOLINE' ],
        },
        public_api: {
            engine_group: [ 'GASOLINE' ],
        },
    },
    {
        category: 'CARS',
        searcher: {
            engine_type: [ 'ENGINE_TURBO', 'ENGINE_NONE' ],
        },
        public_api: {
            engine_group: [ 'TURBO', 'ATMO' ],
        },
    },
    {
        // transmission тут, чтобы произошла конвертация и мы могли проверить сохранность engine_type
        category: 'MOTO',
        searcher: {
            engine_type: [ 'GASOLINE_INJECTOR' ],
            transmission_full: 'AUTOMATIC',
        },
        public_api: {
            engine_type: [ 'GASOLINE_INJECTOR' ],
            transmission: 'AUTOMATIC',
        },
    },
    // euro_class
    {
        searcher: {
            euro_class: [ '1' ],
        },
        public_api: {
            euro_class: [ 'EURO_1' ],
        },
    },
    {
        searcher: {
            euro_class: [ '1', 'GREEN' ],
        },
        public_api: {
            euro_class: [ 'EURO_1', 'EURO_GREEN' ],
        },
    },
    // exchange_status
    {
        searcher: {
            exchange_status: '1',
        },
        public_api: {
            exchange_group: 'POSSIBLE',
        },
    },
    // gear_type
    {
        // engine_type тут, чтобы произошла конвертация и мы могли проверить сохранность gear_type
        category: 'CARS',
        searcher: {
            engine_type: [ 'GASOLINE' ],
            gear_type: 'FORWARD_CONTROL',
        },
        public_api: {
            engine_group: [ 'GASOLINE' ],
            gear_type: 'FORWARD_CONTROL',
        },
    },
    {
        category: 'MOTO',
        searcher: {
            drive_key: 'BACK_DIFFERENTIAL',
        },
        public_api: {
            gear_type: 'BACK_DIFFERENTIAL',
        },
    },
    {
        category: 'MOTO',
        searcher: {
            drive_key: [ 'BACK', 'BACK_DIFFERENTIAL' ],
        },
        public_api: {
            gear_type: [ 'BACK', 'BACK_DIFFERENTIAL' ],
        },
    },
    // haggle
    {
        category: 'TRUCKS',
        searcher: {
            haggle: 'POSSIBLE',
        },
        public_api: {
            haggle: 'HAGGLE_POSSIBLE',
        },
    },
    // in_stock
    {
        searcher: {
            in_stock: 'false',
        },
        public_api: {
            in_stock: 'ANY_STOCK',
        },
    },
    {
        searcher: {
            in_stock: 'true',
        },
        public_api: {
            in_stock: 'IN_STOCK',
        },
    },
    {
        searcher: {
            image: 'false',
        },
        public_api: {
            has_image: 'false',
        },
    },
    // mark-model-nameplate
    {
        searcher: {
            'mark-model-nameplate': [ 'AUDI' ],
        },
        public_api: {
            catalog_filter: [ { mark: 'AUDI' } ],
        },
    },
    {
        searcher: {
            'mark-model-nameplate': [ 'AUDI' ],
            price_to: '100000',
        },
        public_api: {
            catalog_filter: [ { mark: 'AUDI' } ],
            price_to: '100000',
        },
    },
    // moto_category
    {
        category: 'MOTO',
        searcher: {
            moto_category: 'atv',
        },
        public_api: {
            moto_category: 'ATV',
        },
    },
    // moto_color
    {
        category: 'MOTO',
        searcher: {
            moto_color: [ 'FFCC00' ],
        },
        public_api: {
            color: [ 'DEA522' ],
        },
    },
    {
        category: 'MOTO',
        searcher: {
            moto_color: [ '926547', 'FF0000' ],
        },
        public_api: {
            color: [ '200204', 'EE1D19' ],
        },
    },
    // moto_type
    {
        category: 'MOTO',
        searcher: {
            moto_category: 'atv',
            moto_type: 'BUGGI',
        },
        public_api: {
            moto_category: 'ATV',
            atv_type: 'BUGGI',
        },
    },
    {
        category: 'MOTO',
        searcher: {
            moto_category: 'motorcycle',
            moto_type: [ 'SPORTBIKE', 'TOURIST_ENDURO' ],
        },
        public_api: {
            moto_category: 'MOTORCYCLE',
            moto_type: [ 'SPORTBIKE', 'TOURIST_ENDURO' ],
        },
    },
    {
        category: 'MOTO',
        searcher: {
            moto_category: 'snowmobile',
            moto_type: 'CHILDISH',
        },
        public_api: {
            moto_category: 'SNOWMOBILE',
            snowmobile_type: 'CHILDISH',
        },
    },
    // owners_count
    {
        searcher: {
            owners_count: '1',
        },
        public_api: {
            owners_count_group: 'ONE',
        },
    },
    {
        searcher: {
            owners_count: '2-',
        },
        public_api: {
            owners_count_group: 'LESS_THAN_TWO',
        },
    },
    {
        searcher: {
            owning_time: '0_12',
        },
        public_api: {
            owning_time_group: 'LESS_THAN_YEAR',
        },
    },
    // owning_time
    {
        searcher: {
            owning_time: '12_36',
        },
        public_api: {
            owning_time_group: 'FROM_1_TO_3',
        },
    },
    {
        searcher: {
            owning_time: '36_',
        },
        public_api: {
            owning_time_group: 'MORE_THAN_3',
        },
    },
    // page_num_offers
    {
        searcher: {
            page_num_offers: '2',
        },
        public_api: {
            page: '2',
        },
    },
    // saddle_height
    {
        category: 'TRUCKS',
        searcher: {
            saddle_height: '128',
        },
        public_api: {
            saddle_height: 'SH_128',
        },
    },
    {
        category: 'TRUCKS',
        searcher: {
            saddle_height: [ '128', '150' ],
        },
        public_api: {
            saddle_height: [ 'SH_128', 'SH_150' ],
        },
    },
    // strokes
    {
        category: 'MOTO',
        searcher: {
            strokes: [ '2' ],
        },
        public_api: {
            strokes: [ 'STROKES_2' ],
        },
    },
    // transmission_full
    {
        category: 'CARS',
        searcher: {
            transmission_full: 'MECHANICAL',
        },
        public_api: {
            transmission: 'MECHANICAL',
        },
    },
    {
        category: 'CARS',
        searcher: {
            transmission_full: 'AUTO_AUTOMATIC',
        },
        public_api: {
            transmission: 'AUTOMATIC',
        },
    },
    {
        category: 'CARS',
        searcher: {
            transmission_full: [ 'AUTO_AUTOMATIC', 'AUTO_ROBOT', 'AUTO_VARIATOR' ],
        },
        public_api: {
            transmission: [ 'AUTOMATIC', 'ROBOT', 'VARIATOR' ],
        },
    },
    {
        category: 'CARS',
        searcher: {
            transmission_full: [ 'AUTO_ROBOT', 'AUTO_VARIATOR', 'MECHANICAL' ],
        },
        public_api: {
            transmission: [ 'ROBOT', 'VARIATOR', 'MECHANICAL' ],
        },
    },
    {
        category: 'MOTO',
        searcher: {
            transmission_full: [ '4', '6_FORWARD_AND_BACK', 'AUTOMATIC_2_SPEED' ],
        },
        public_api: {
            transmission: [ 'TRANSMISSION_4', 'TRANSMISSION_6_FORWARD_AND_BACK', 'AUTOMATIC_2_SPEED' ],
        },
    },
    {
        category: 'MOTO',
        searcher: {
            transmission_full: '8',
        },
        public_api: {
            transmission: 'TRANSMISSION_8',
        },
    },
    // truck_color
    {
        category: 'TRUCKS',
        searcher: {
            truck_color: [ 'FFCC00', '926547', 'FF0000' ],
        },
        public_api: {
            color: [ 'DEA522', '200204', 'EE1D19' ],
        },
    },
    // warranty_status
    {
        searcher: {
            warranty_status: '1',
        },
        public_api: {
            with_warranty: 'true',
        },
    },
    {
        searcher: {
            video: 'true',
        },
        public_api: {
            has_video: 'true',
        },
    },
    {
        searcher: {
            sort_offers: 'fresh_relevance',
        },
        public_api: {
            sort: 'fresh_relevance',
        },
    },
    // wheel_drive
    {
        category: 'TRUCKS',
        searcher: {
            wheel_drive: '4x4',
        },
        public_api: {
            wheel_drive: 'WD_4x4',
        },
    },
    {
        category: 'TRUCKS',
        searcher: {
            wheel_drive: [ '6x4' ],
        },
        public_api: {
            wheel_drive: [ 'WD_6x4' ],
        },
    },
];
