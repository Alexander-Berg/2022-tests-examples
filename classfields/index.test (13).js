const preparer = require('./listing');

it('должен удалить дефолтные значения', () => {
    expect(preparer({
        cars_params: {
            body_type_group: [ 'ANY_BODY' ],
            seats_group: 'ANY_SEATS',
        },
        with_warranty: false,
        currency: 'RUR',
        state: [ 'NEW', 'USED' ],
        custom_state_key: [ 'CLEARED' ],
        exchange_status: [ 'NO_EXCHANGE' ],
        state_group: 'ALL',
        exchange_group: 'NO_EXCHANGE',
        seller_group: [ 'ANY_SELLER' ],
        damage_group: 'NOT_BEATEN',
        owners_count_group: 'ANY_COUNT',
        owning_time_group: 'ANY_TIME',
        customs_state_group: 'CLEARED',
    })).toEqual({
        section: 'all',
    });
});

it('должен сохранить has_image=false', () => {
    expect(preparer({ has_image: false })).toEqual({ has_image: false });
});

it('должен удалить has_image=true', () => {
    expect(preparer({ has_image: true })).toEqual({});
});

describe('has_video', () => {
    it('должен преобразовать search_tag:["video"] в has_video:true', () => {
        expect(preparer({ search_tag: [ 'video' ] })).toEqual({
            has_video: true,
        });
    });

    it('должен преобразовать { search_tag: [ "video", "vin_checked" ] } в { has_video:true, search_tag: [ "vin_checked" ] }', () => {
        expect(preparer({ search_tag: [ 'video', 'vin_checked' ] })).toEqual({
            has_video: true,
            search_tag: [ 'vin_checked' ],
        });
    });
});

describe('online_view_available', () => {
    it('должен преобразовать search_tag:["online_view_available"] в online_view:true', () => {
        expect(preparer({ search_tag: [ 'online_view_available' ] })).toEqual({
            online_view: true,
        });
    });

    it('должен преобразовать { search_tag: [ "online_view_available", "vin_checked" ] } в { online_view:true, search_tag: [ "vin_checked" ] }', () => {
        expect(preparer({ search_tag: [ 'online_view_available', 'vin_checked' ] })).toEqual({
            online_view: true,
            search_tag: [ 'vin_checked' ],
        });
    });
});

describe('search_tag certificate', () => {
    // eslint-disable-next-line max-len
    it('должен преобразовать { search_tag: [ "certificate_manufacturer,vin_checked" ] } в { search_tag: [ "certificate_manufacturer", "vin_checked" ] }', () => {
        expect(preparer({ search_tag: [ 'certificate_manufacturer,vin_checked' ] })).toEqual({
            search_tag: [ 'certificate_manufacturer', 'vin_checked' ],
        });
    });
});

it('должен преобразовать { autoru_billing_service_type: [ "special" ] } в { special: "true" }', () => {
    expect(preparer({ autoru_billing_service_type: [ 'special' ] })).toEqual({
        special: true,
    });
});

it('должен удалить feeding_type', () => {
    expect(preparer({
        cars_params: {
            feeding_type: [ 'TURBO', 'NONE' ],
            engine_group: [ 'TURBO', 'ATMO' ],
        },
    })).toEqual({
        engine_group: [ 'TURBO', 'ATMO' ],
    });
});

it('должен удалить owners_count_*', () => {
    expect(preparer({
        cars_params: {
            owners_count_from: 1,
            owners_count_group: 'ONE',
            owners_count_to: 1,
        },
    })).toEqual({
        owners_count_group: 'ONE',
    });
});

it('должен удалить owning_time_*', () => {
    expect(preparer({
        cars_params: {
            owning_time_from: 12,
            owning_time_group: 'FROM_1_TO_3',
            owning_time_to: 36,
        },
    })).toEqual({
        owning_time_group: 'FROM_1_TO_3',
    });
});

it('должен удалить exchange_status', () => {
    expect(preparer({
        exchange_status: [ 'POSSIBLE' ],
        exchange_group: 'POSSIBLE',
    })).toEqual({
        exchange_group: 'POSSIBLE',
    });
});

it('должен привести configuration_id к строке', () => {
    expect(preparer({
        configuration_id: [ '7754685' ],
    })).toEqual({
        configuration_id: '7754685',
    });
});

it('должен привести tech_param_id к строке', () => {
    expect(preparer({
        tech_param_id: [ '7754686' ],
    })).toEqual({
        tech_param_id: '7754686',
    });
});

describe('in_stock', () => {
    it('должен удалить in_stock=ANY_STOCK для section=all', () => {
        expect(preparer({
            in_stock: 'ANY_STOCK',
            state_group: 'ALL',
        })).toEqual({
            section: 'all',
        });
    });

    it('должен удалить in_stock=ANY_STOCK для section=used', () => {
        expect(preparer({
            in_stock: 'ANY_STOCK',
            state_group: 'USED',
        })).toEqual({
            section: 'used',
        });
    });

    it('должен удалить in_stock=ANY_STOCK для section=new', () => {
        expect(preparer({
            in_stock: 'ANY_STOCK',
            state_group: 'new',
        })).toEqual({
            section: 'new',
        });
    });

    it('не должен удалить in_stock=IN_STOCK для section=new', () => {
        expect(preparer({
            in_stock: 'IN_STOCK',
            state_group: 'NEW',
        })).toEqual({
            in_stock: 'IN_STOCK',
            section: 'new',
        });
    });

    it('не должен удалить in_stock=IN_STOCK для section=used', () => {
        expect(preparer({
            in_stock: 'IN_STOCK',
            state_group: 'USED',
        })).toEqual({
            in_stock: 'IN_STOCK',
            section: 'used',
        });
    });
});

describe('addDo', () => {
    it('должен свопнуть price_to и do и удалить price, если есть do', () => {
        expect(preparer(
            {
                price_to: 1,
            },
            null,
            { 'do': 2 },
        )).toEqual({
            'do': 2,
        });
    });

    it('ничего не должен делать, если нет do', () => {
        expect(preparer({
            price_to: 1,
        })).toEqual({
            price_to: 1,
        });
    });
});
