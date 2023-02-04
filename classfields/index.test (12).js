const index = require('./index');

describe('equipment', () => {
    it('должен убрать опции с флагом false в cars', () => {
        const result = index({
            category: 'cars',
            car_info: {
                equipment: {
                    option1: true,
                    option2: false,
                },
            },
        });

        expect(result.equipment).toEqual({
            option1: true,
        });
    });

    it('должен убрать опции с флагом false в moto', () => {
        const result = index({
            category: 'moto',
            moto_info: {
                equipment: {
                    option1: true,
                    option2: false,
                },
            },
        });

        expect(result).toEqual({
            equipment: {
                option1: true,
            },
        });
    });

    it('должен убрать опции с флагом false в trucks', () => {
        const result = index({
            category: 'trucks',
            truck_info: {
                equipment: {
                    option1: true,
                    option2: false,
                },
            },
        });

        expect(result).toEqual({
            equipment: {
                option1: true,
            },
        });
    });

    it('не должен упасть, если опций нет', () => {
        const result = index({
            category: 'cars',
            car_info: {},
        });

        expect(result.equipment).toEqual({});
    });
});
