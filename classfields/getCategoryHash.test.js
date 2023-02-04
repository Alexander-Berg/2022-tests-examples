const getCategoryHash = require('./getCategoryHash');

describe('должен правильно составлять хэш категории', () => {
    const tariffs = {
        CARS_USED: {
            category: 'CARS',
            section: [ 'USED' ],
        },
        CARS_NEW: {
            category: 'CARS',
            section: [ 'NEW' ],
        },
        TRUCKS_COMMERCIAL: {
            category: 'TRUCKS',
            truck_class: 'COMMERCIAL',
            section: [ 'USED', 'NEW' ],
        },
        TRUCKS_LCV_USED: {
            category: 'TRUCKS',
            truck_class: 'LCV',
            section: [ 'USED' ],
        },
        MOTO: {
            category: 'MOTO',
            section: [ 'USED', 'NEW' ],
        },
    };

    it('CARS_USED', () => {
        expect(getCategoryHash(tariffs.CARS_USED)).toBe('CARS_USED');
    });

    it('CARS_NEW', () => {
        expect(getCategoryHash(tariffs.CARS_NEW)).toBe('CARS_NEW');
    });

    it('TRUCKS_COMMERCIAL', () => {
        expect(getCategoryHash(tariffs.TRUCKS_COMMERCIAL)).toBe('TRUCKS_COMMERCIAL');
    });

    it('TRUCKS_LCV_USED', () => {
        expect(getCategoryHash(tariffs.TRUCKS_LCV_USED)).toBe('TRUCKS_LCV_USED');
    });

    it('MOTO', () => {
        expect(getCategoryHash(tariffs.MOTO)).toBe('MOTO');
    });
});
