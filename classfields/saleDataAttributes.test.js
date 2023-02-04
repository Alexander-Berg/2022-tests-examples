const saleDataAttributes = require('./saleDataAttributes');

it('должен вернуть sale-data-attributes для объявления (общий тест)', () => {
    const offer = {
        category: 'cars',
        documents: {
            year: 2018,
        },
        price_info: {
            RUR: 10000,
        },
        section: 'used',
        state: {
            image_urls: [
                { sizes: { '1200x900': 'image1' } },
                { sizes: { '1200x900': 'image2' } },
            ],
            mileage: 10000,
        },
        sub_category: 'cars',
        vehicle_info: {
            configuration: {
                body_type: 'SEDAN',
            },
            mark_info: {
                code: 'MARK_CODE',
                name: 'Mark name',
                numeric_id: 42,
            },
            model_info: {
                code: 'MODEL_CODE',
                name: 'Model name',
            },
            super_gen: {
                price_segment: 'MEDIUM',
            },
            tech_param: {
                engine_type: 'DIESEL',
                power: 107,
                transmission: 'AUTOMATIC',
            },
        },
    };

    expect(saleDataAttributes(offer)).toEqual({
        asciiCat: 'cars',
        category: 'cars',
        'engine-type': 'DIESEL',
        image: 'image1',
        'km-age': 10000,
        mark: 'MARK_CODE',
        markName: 'Mark name',
        model: 'MODEL_CODE',
        modelName: 'Model name',
        power: 107,
        price: 10000,
        puid2: '42',
        puid10: '1',
        routeCategory: 'cars',
        segment: 'MEDIUM',
        state: 'used',
        transmission: 'AUTOMATIC',
        type: '',
        year: 2018,
    });
});

it('должен вернуть sale-data-attributes с type: "suv" для внедорожника', () => {
    const offer = {
        category: 'cars',
        documents: {
            year: 2018,
        },
        price_info: {
            RUR: 10000,
        },
        section: 'used',
        state: {
            image_urls: [
                { sizes: { '1200x900': 'image1' } },
                { sizes: { '1200x900': 'image2' } },
            ],
            mileage: 10000,
        },
        sub_category: 'cars',
        vehicle_info: {
            configuration: {
                body_type: 'ALLROAD_5_DOORS',
            },
            mark_info: {
                code: 'MARK_CODE',
                name: 'Mark name',
                numeric_id: '42',
            },
            model_info: {
                code: 'MODEL_CODE',
                name: 'Model name',
            },
            super_gen: {
                price_segment: 'MEDIUM',
            },
            tech_param: {
                engine_type: 'DIESEL',
                power: 107,
                transmission: 'AUTOMATIC',
            },
        },
    };

    expect(saleDataAttributes(offer)).toEqual({
        asciiCat: 'cars',
        category: 'cars',
        'engine-type': 'DIESEL',
        image: 'image1',
        'km-age': 10000,
        mark: 'MARK_CODE',
        markName: 'Mark name',
        model: 'MODEL_CODE',
        modelName: 'Model name',
        power: 107,
        price: 10000,
        puid2: '42',
        puid10: '1',
        routeCategory: 'cars',
        segment: 'MEDIUM',
        state: 'used',
        transmission: 'AUTOMATIC',
        type: 'suv',
        year: 2018,
    });
});

it('должен вернуть sale-data-attributes с абсолютным урлом на фотку', () => {
    const offer = {
        category: 'cars',
        documents: {
            year: 2018,
        },
        price_info: {
            RUR: 10000,
        },
        section: 'used',
        state: {
            image_urls: [
                { sizes: { '1200x900': '//avatars.mds.yandex.net/get-autoru-all/1685614/019bb6af771f8fe9b8eaf12f3f3e22fe/1200x900' } },
            ],
            mileage: 10000,
        },
        sub_category: 'cars',
        vehicle_info: {
            configuration: {
                body_type: 'ALLROAD_5_DOORS',
            },
            mark_info: {
                code: 'MARK_CODE',
                name: 'Mark name',
                numeric_id: 42,
            },
            model_info: {
                code: 'MODEL_CODE',
                name: 'Model name',
            },
            super_gen: {
                price_segment: 'MEDIUM',
            },
            tech_param: {
                engine_type: 'DIESEL',
                power: 107,
                transmission: 'AUTOMATIC',
            },
        },
    };

    expect(saleDataAttributes(offer)).toEqual({
        asciiCat: 'cars',
        category: 'cars',
        'engine-type': 'DIESEL',
        image: 'https://avatars.mds.yandex.net/get-autoru-all/1685614/019bb6af771f8fe9b8eaf12f3f3e22fe/1200x900',
        'km-age': 10000,
        mark: 'MARK_CODE',
        markName: 'Mark name',
        model: 'MODEL_CODE',
        modelName: 'Model name',
        power: 107,
        price: 10000,
        puid2: '42',
        puid10: '1',
        routeCategory: 'cars',
        segment: 'MEDIUM',
        state: 'used',
        transmission: 'AUTOMATIC',
        type: 'suv',
        year: 2018,
    });
});

it('должен вернуть sale-data-attributes image:null, если нет фоток', () => {
    const offer = {
        category: 'cars',
        documents: {
            year: 2018,
        },
        price_info: {
            RUR: 10000,
        },
        section: 'used',
        state: {
            mileage: 10000,
        },
        sub_category: 'cars',
        vehicle_info: {
            configuration: {
                body_type: 'SEDAN',
            },
            mark_info: {
                code: 'MARK_CODE',
                name: 'Mark name',
                numeric_id: 42,
            },
            model_info: {
                code: 'MODEL_CODE',
                name: 'Model name',
            },
            super_gen: {
                price_segment: 'MEDIUM',
            },
            tech_param: {
                engine_type: 'DIESEL',
                power: 107,
                transmission: 'AUTOMATIC',
            },
        },
    };

    expect(saleDataAttributes(offer)).toEqual({
        asciiCat: 'cars',
        category: 'cars',
        'engine-type': 'DIESEL',
        image: null,
        'km-age': 10000,
        mark: 'MARK_CODE',
        markName: 'Mark name',
        model: 'MODEL_CODE',
        modelName: 'Model name',
        power: 107,
        puid2: '42',
        puid10: '1',
        price: 10000,
        routeCategory: 'cars',
        segment: 'MEDIUM',
        state: 'used',
        transmission: 'AUTOMATIC',
        type: '',
        year: 2018,
    });
});

it('должен вернуть в category топовую категория', () => {
    const offer = {
        category: 'trucks',
        sub_category: 'lcv',
        vehicle_info: {},
    };

    expect(saleDataAttributes(offer)).toHaveProperty('category', 'trucks');
});
