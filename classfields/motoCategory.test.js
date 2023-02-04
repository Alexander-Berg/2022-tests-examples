const motoCategory = require('./motoCategory');

describe('atv', () => {
    it('должен вернуть тип вездехода "Утилитарный"', () => {
        expect(motoCategory({
            vehicle_info: {
                atv_type: 'UTILITARIAN',
                moto_category: 'ATV',
            },
        })).toEqual('Утилитарный');
    });
});

describe('motorcycle', () => {
    it('должен вернуть тип мотоцикла "Туризм"', () => {
        expect(motoCategory({
            vehicle_info: {
                moto_category: 'MOTORCYCLE',
                moto_type: 'TOURISM',
            },
        })).toEqual('Туризм');
    });

    it('должен вернуть тип мотоцикла "Трайк"', () => {
        expect(motoCategory({
            vehicle_info: {
                moto_category: 'MOTORCYCLE',
                moto_type: 'TRIKE',
            },
        })).toEqual('Трайки');
    });
});

describe('scooters', () => {
    it('не должен вернуть тип скутера', () => {
        expect(motoCategory({
            vehicle_info: {
                moto_category: 'SCOOTERS',
            },
        })).toBeNull();
    });
});

describe('snowmobile', () => {
    it('должен вернуть тип снегохода "Спортивный горный"', () => {
        expect(motoCategory({
            vehicle_info: {
                moto_category: 'SNOWMOBILE',
                snowmobile_type: 'SPORTS_MOUNTAIN',
            },
        })).toEqual('Спортивный горный');
    });
});
