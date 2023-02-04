import type { ModelOffer } from '../../models';

import get_mileage from './get_mileage';

it('если есть пробег вернет его с размерностью', () => {
    const result = get_mileage({
        state: {
            mileage: 42000,
        },
    } as ModelOffer);

    expect(result).toBe('42 000 км');
});

describe('если нет пробега', () => {
    it('для новых вернет "новый"', () => {
        const result = get_mileage({
            state: {},
            section: 'NEW',
        } as ModelOffer);

        expect(result).toBe('новый');
    });

    it('для б/у вернет "пробег не указан"', () => {
        const result = get_mileage({
            state: {},
            section: 'USED',
        } as ModelOffer);

        expect(result).toBe('пробег не указан');
    });
});

describe('для особых категорий', () => {
    it('вернет моточасы если они есть', () => {
        const result = get_mileage({
            state: {},
            truck_info: {
                truck_category: 'AGRICULTURAL',
                operating_hours: 42,
            },
        } as ModelOffer);

        expect(result).toBe('42 моточаса');
    });

    it('вернет "пробег не указан" если моточасов нет', () => {
        const result = get_mileage({
            state: {},
            truck_info: {
                truck_category: 'AGRICULTURAL',
            },
        } as ModelOffer);

        expect(result).toBe('пробег не указан');
    });
});
