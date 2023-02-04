const normalizeParamsAfterRouter = require('auto-core/router/libs/normalizeParamsAfterRouter');

it('should do nothing with empty object', () => {
    expect(normalizeParamsAfterRouter({})).toEqual({});
});

it('should pass array "mark-model-nameplate"', () => {
    expect(normalizeParamsAfterRouter({
        'mark-model-nameplate': [ 'OPEL' ],
    })).toEqual({
        'mark-model-nameplate': [ 'OPEL' ],
    });
});

it('should preserve order "mark-model-nameplate"', () => {
    expect(normalizeParamsAfterRouter({
        'mark-model-nameplate': [ 'OPEL', '' ],
    })).toEqual({
        'mark-model-nameplate': [ 'OPEL', '' ],
    });
});

it('should cast string "mark-model-nameplate[]" to "mark-model-nameplate"', () => {
    expect(normalizeParamsAfterRouter({
        'mark-model-nameplate[]': 'OPEL',
    })).toEqual({
        'mark-model-nameplate': [ 'OPEL' ],
    });
});

it('should cast array "mark-model-nameplate[]" to "mark-model-nameplate"', () => {
    expect(normalizeParamsAfterRouter({
        'mark-model-nameplate[]': [ 'OPEL' ],
    })).toEqual({
        'mark-model-nameplate': [ 'OPEL' ],
    });
});

it('should cast string "mark-model-nameplate" to array "mark-model-nameplate"', () => {
    expect(normalizeParamsAfterRouter({
        'mark-model-nameplate': 'OPEL',
    })).toEqual({
        'mark-model-nameplate': [ 'OPEL' ],
    });
});

describe('\\d{4}-year', () => {
    it('должен преобразовать year="2018-year" в year_from=2018&year_to=2018', () => {
        expect(normalizeParamsAfterRouter({
            year: '2018-year',
        })).toEqual({
            year_from: '2018',
            year_to: '2018',
        });
    });
});

describe('body-\\w+', () => {
    it('должен преобразовать "/body-sedan/" в "body_type_group=SEDAN"', () => {
        expect(normalizeParamsAfterRouter({
            body_type_sef: 'body-sedan',
        })).toEqual({
            body_type_group: [ 'SEDAN' ],
        });
    });
    it('должен преобразовать "/body-hatchback/" в "body_type_group=[ HATCHBACK, HATCHBACK_3_DOORS, HATCHBACK_5_DOORS, LIFTBACK ]"', () => {
        expect(normalizeParamsAfterRouter({
            body_type_sef: 'body-hatchback',
        })).toEqual({
            body_type_group: [ 'HATCHBACK', 'HATCHBACK_3_DOORS', 'HATCHBACK_5_DOORS', 'LIFTBACK' ],
        });
    });
});

describe('transmission-\\w+', () => {
    it('должен преобразовать "/transmission-mechanical/" в "transmission=MECHANICAL"', () => {
        expect(normalizeParamsAfterRouter({
            transmission_sef: 'transmission-mechanical',
        })).toEqual({
            transmission: [ 'MECHANICAL' ],
        });
    });
    it('должен преобразовать "/transmission-automatic/" в "transmission=[ AUTO, AUTOMATIC, VARIATOR, ROBOT ]"', () => {
        expect(normalizeParamsAfterRouter({
            transmission_sef: 'transmission-automatic',
        })).toEqual({
            transmission: [ 'AUTOMATIC', 'AUTO', 'ROBOT', 'VARIATOR' ],
        });
    });
});

describe('color-\\w+', () => {
    it('должен преобразовать "/color-chernyj/" в "color=040001"', () => {
        expect(normalizeParamsAfterRouter({
            color_sef: 'color-chernyj',
        })).toEqual({
            color: [ '040001' ],
        });
    });
});

describe('engine-\\w+', () => {
    it('должен преобразовать "/engine-benzin/" в "engine_type=GASOLINE"', () => {
        expect(normalizeParamsAfterRouter({
            engine_type_sef: 'engine-benzin',
        })).toEqual({
            engine_group: [ 'GASOLINE' ],
        });
    });
});

describe('displacement-\\w+', () => {
    it('должен преобразовать "/displacement-3000/" в "displacement_from=3000&displacement_to=3000"', () => {
        expect(normalizeParamsAfterRouter({
            displacement_sef: 'displacement-3000',
        })).toEqual({
            displacement_from: '3000',
            displacement_to: '3000',
        });
    });
});

describe('seats-\\w+', () => {
    it('должен преобразовать "/seats-5/" в "catalog_equipment[]=seats-5"', () => {
        expect(normalizeParamsAfterRouter({
            seats_sef: 'seats-5',
        })).toEqual({
            catalog_equipment: [ 'seats-5' ],
        });
    });
});

describe('drive-\\w+', () => {
    it('должен преобразовать "/drive-4x4_wheel/" в "gear_type=ALL_WHEEL_DRIVE"', () => {
        expect(normalizeParamsAfterRouter({
            drive_sef: 'drive-4x4_wheel',
        })).toEqual({
            gear_type: [ 'ALL_WHEEL_DRIVE' ],
        });
    });
});

describe('geo_id', () => {
    describe('строка', () => {
        it('должен оставить валидный ID как есть', () => {
            expect(normalizeParamsAfterRouter({
                geo_id: '213',
            })).toEqual({
                geo_id: [ 213 ],
            });
        });

        it('должен удалить geo_id=0', () => {
            expect(normalizeParamsAfterRouter({
                geo_id: '0',
            })).toEqual({});
        });

        it('должен удалить нечисловой geo_id', () => {
            expect(normalizeParamsAfterRouter({
                geo_id: 'foo',
            })).toEqual({});
        });
    });

    describe('число', () => {
        it('должен оставить валидный ID как есть', () => {
            expect(normalizeParamsAfterRouter({
                geo_id: 213,
            })).toEqual({
                geo_id: [ 213 ],
            });
        });

        it('должен удалить geo_id=0', () => {
            expect(normalizeParamsAfterRouter({
                geo_id: 0,
            })).toEqual({});
        });
    });

    describe('массив', () => {
        it('должен оставить валидный ID как есть', () => {
            expect(normalizeParamsAfterRouter({
                geo_id: [ '213' ],
            })).toEqual({
                geo_id: [ 213 ],
            });
        });

        it('должен удалить geo_id=0', () => {
            expect(normalizeParamsAfterRouter({
                geo_id: [ '0', '213' ],
            })).toEqual({
                geo_id: [ 213 ],
            });
        });

        it('должен удалить нечисловой geo_id', () => {
            expect(normalizeParamsAfterRouter({
                geo_id: [ 'foo', '213' ],
            })).toEqual({
                geo_id: [ 213 ],
            });
        });

        it('должен удалить пустой невалидный массив', () => {
            expect(normalizeParamsAfterRouter({
                geo_id: [ 'foo' ],
            })).toEqual({});
        });
    });
});

describe('catalog_filter', () => {
    it('должен преобразовать mark_model_nameplate в catalog_filter', () => {
        expect(normalizeParamsAfterRouter({
            category: 'cars',
            section: 'all',
            mark_model_nameplate: [ 'AUDI#A1##111', 'AUDI#A1##222' ],
        }, 'listing-full')).toEqual({
            category: 'cars',
            section: 'all',
            mark_model_nameplate: [ 'AUDI#A1##111', 'AUDI#A1##222' ],
            catalog_filter: [
                { mark: 'AUDI', model: 'A1', generation: '111' },
                { mark: 'AUDI', model: 'A1', generation: '222' },
            ],
        });
    });

    it('должен преобразовать catalog_filter массив строк в массив объектов', () => {
        expect(normalizeParamsAfterRouter({
            category: 'cars',
            section: 'all',
            catalog_filter: [ 'mark=AUDI,model=A1,generation=111', 'mark=AUDI,model=A1,generation=222' ],
        }, 'reviews-listing')).toEqual({
            category: 'cars',
            section: 'all',
            catalog_filter: [
                { mark: 'AUDI', model: 'A1', generation: '111' },
                { mark: 'AUDI', model: 'A1', generation: '222' },
            ],
        });
    });

    it('не должен преобразовать catalog_filter массив объектов', () => {
        expect(normalizeParamsAfterRouter({
            category: 'cars',
            section: 'all',
            catalog_filter: [
                { mark: 'AUDI', model: 'A1', generation: '111' },
                { mark: 'AUDI', model: 'A1', generation: '222' },
            ],
        }, 'reviews-listing')).toEqual({
            category: 'cars',
            section: 'all',
            catalog_filter: [
                { mark: 'AUDI', model: 'A1', generation: '111' },
                { mark: 'AUDI', model: 'A1', generation: '222' },
            ],
        });
    });

    it('должен преобразовать mark model super_gen в catalog_filter и удалить их для листинга', () => {
        expect(normalizeParamsAfterRouter({
            category: 'cars',
            section: 'all',
            mark: 'audi',
            model: 'a1',
            super_gen: '111',
        }, 'listing')).toEqual({
            category: 'cars',
            section: 'all',
            catalog_filter: [
                { mark: 'AUDI', model: 'A1', generation: '111' },
            ],
        });
    });

    it('должен преобразовать mark, model, super_gen, configuration_id в catalog_filter и удалить их для карточки группы', () => {
        expect(normalizeParamsAfterRouter({
            category: 'cars',
            section: 'new',
            mark: 'audi',
            model: 'a1',
            super_gen: '111',
            configuration_id: '222',
        }, 'card-group')).toEqual({
            category: 'cars',
            section: 'new',
            catalog_filter: [
                { mark: 'AUDI', model: 'A1', generation: '111', configuration: '222' },
            ],
        });
    });

    it('должен использовать текущий каталог фильтр для карточки группы, если в нем одно значение', () => {
        expect(normalizeParamsAfterRouter({
            category: 'cars',
            section: 'new',
            mark: 'audi',
            model: 'a1',
            super_gen: '111',
            configuration_id: '222',
            catalog_filter: 'mark=AUDI,model=A1,generation=111,configuration=222,tech_param=333,complectation_name=super',
        }, 'card-group')).toEqual({
            category: 'cars',
            section: 'new',
            catalog_filter: [
                {
                    mark: 'AUDI',
                    model: 'A1',
                    generation: '111',
                    configuration: '222',
                    tech_param: '333',
                    complectation_name: 'super',
                },
            ],
        });
    });

    it('должен использовать текущий каталог фильтр для карточки группы, если в нем несколько значений', () => {
        expect(normalizeParamsAfterRouter({
            category: 'cars',
            section: 'new',
            mark: 'audi',
            model: 'a1',
            super_gen: '111',
            configuration_id: '222',
            catalog_filter: [
                'mark=AUDI,model=A1,generation=111,configuration=222,tech_param=333,complectation_name=super',
                'mark=AUDI,model=A1,generation=111,configuration=222,tech_param=444,complectation_name=super',
            ],
        }, 'card-group')).toEqual({
            category: 'cars',
            section: 'new',
            catalog_filter: [
                {
                    mark: 'AUDI',
                    model: 'A1',
                    generation: '111',
                    configuration: '222',
                    tech_param: '333',
                    complectation_name: 'super',
                },
                {
                    mark: 'AUDI',
                    model: 'A1',
                    generation: '111',
                    configuration: '222',
                    tech_param: '444',
                    complectation_name: 'super',
                },
            ],
        });
    });

    it('должен сформировать новый каталог фильтр для карточки группы из параметров, ' +
       'если в текущем каталог фильтре не хватает необходимых данных (марка, модель, поколение, конфигурация)', () => {
        expect(normalizeParamsAfterRouter({
            category: 'cars',
            section: 'new',
            mark: 'audi',
            model: 'a1',
            super_gen: '111',
            configuration_id: '222',
            catalog_filter: 'mark=AUDI,model=A1',
        }, 'card-group')).toEqual({
            category: 'cars',
            section: 'new',
            catalog_filter: [
                { mark: 'AUDI', model: 'A1', generation: '111', configuration: '222' },
            ],
        });
    });

    it('должен удалить exclude_catalog_filter, если он пустой', () => {
        expect(normalizeParamsAfterRouter({
            category: 'cars',
            section: 'new',
            exclude_catalog_filter: '',
            catalog_filter: '',
        }, 'listing-full')).toEqual({
            category: 'cars',
            section: 'new',
        });
    });
});

describe('category', () => {
    describe('desktop', () => {
        it('правильно отрабатывает для категории cars', () => {
            expect(normalizeParamsAfterRouter({
                category: 'cars',
                section: 'all',
            }, 'card',
            )).toEqual({
                category: 'cars',
                section: 'all',
            });
        });

        it('правильно отрабатывает для категории moto', () => {
            expect(normalizeParamsAfterRouter({
                category: 'moto',
                section: 'all',
            }, 'card',
            )).toEqual({
                category: 'moto',
                section: 'all',
                moto_category: 'moto',
            });
        });

        it('правильно отрабатывает для категории trucks', () => {
            expect(normalizeParamsAfterRouter({
                category: 'trucks',
                section: 'all',
            }, 'card',
            )).toEqual({
                category: 'trucks',
                section: 'all',
                truck_category: 'trucks',
            });
        });

        it('правильно отрабатывает для старой категории commercial', () => {
            expect(normalizeParamsAfterRouter({
                category: 'commercial',
                section: 'all',
            }, 'card',
            )).toEqual({
                category: 'trucks',
                section: 'all',
                truck_category: 'commercial',
            });
        });
    });

    describe('mobile', () => {
        it('правильно отрабатывает для категории cars', () => {
            expect(normalizeParamsAfterRouter({
                category: 'cars',
                section: 'all',
            }, 'card',
            )).toEqual({
                category: 'cars',
                section: 'all',
            });
        });

        it('правильно отрабатывает для подкатегорий moto', () => {
            expect(normalizeParamsAfterRouter({
                category: 'motorcycle',
                section: 'all',
            }, 'card',
            )).toEqual({
                category: 'moto',
                moto_category: 'motorcycle',
                section: 'all',
            });
        });

        it('правильно отрабатывает для подкатегорий trucks', () => {
            expect(normalizeParamsAfterRouter({
                category: 'lcv',
                section: 'all',
            }, 'card',
            )).toEqual({
                category: 'trucks',
                truck_category: 'lcv',
                section: 'all',
            });
        });
    });
});

describe('proauto-report', () => {
    it('должен понять, что параметр history_entity_id это offer_id', () => {
        expect(normalizeParamsAfterRouter(
            { history_entity_id: '111-aaa' },
            'proauto-report',
        )).toEqual({
            history_entity_id: '111-aaa',
            offer_id: '111-aaa',
        });
    });

    it('должен понять, что параметр history_entity_id это валидный госномер', () => {
        expect(normalizeParamsAfterRouter(
            { history_entity_id: 'a111aa11' },
            'proauto-report',
        )).toEqual({
            history_entity_id: 'a111aa11',
            vin_or_license_plate: 'a111aa11',
        });
    });

    it('должен понять, что параметр history_entity_id это валидный вин', () => {
        expect(normalizeParamsAfterRouter(
            { history_entity_id: 'JH4DA1750JS009856' },
            'proauto-report',
        )).toEqual({
            history_entity_id: 'JH4DA1750JS009856',
            vin_or_license_plate: 'JH4DA1750JS009856',
        });
    });

    it('должен понять, что параметр history_entity_id это валидный японский номер кузова', () => {
        expect(normalizeParamsAfterRouter(
            { history_entity_id: 'JH4D-A150JS00' },
            'proauto-report',
        )).toEqual({
            history_entity_id: 'JH4D-A150JS00',
            vin_or_license_plate: 'JH4D-A150JS00',
        });
    });

    it('должен понять, что параметр history_entity_id это валидный uuid ака номер заказа', () => {
        expect(normalizeParamsAfterRouter(
            { history_entity_id: '8486a639-0a21-49d3-a906-3f877b8a1e99' },
            'proauto-report',
        )).toEqual({
            history_entity_id: '8486a639-0a21-49d3-a906-3f877b8a1e99',
            order_id: '8486a639-0a21-49d3-a906-3f877b8a1e99',
        });
    });

    it('должен понять, что параметр history_entity_id это что-то невалидное', () => {
        expect(normalizeParamsAfterRouter(
            { history_entity_id: '111aaa' },
            'proauto-report',
        )).toEqual({
            history_entity_id: '111aaa',
        });
    });
});
