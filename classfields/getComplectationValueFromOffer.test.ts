import type { Offer as RawOffer } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';

import getComplectationValueFromOffer from './getComplectationValueFromOffer';

describe('getComplectationValueFromOffer', () => {
    it('возвращает валидное значение, если есть оффер с данными из car_info', () => {
        const mockOffer = createMockOffer({
            configurationId: '1',
            complectationId: '2',
            techId: '3',
        });

        expect(getComplectationValueFromOffer(mockOffer)).toBe('1_2_3');
    });

    it('возвращает пустую строку, если нет car_info', () => {
        expect(getComplectationValueFromOffer({} as RawOffer)).toBe('');
    });

    it('возвращает пустую строку, если нет оффера', () => {
        expect(getComplectationValueFromOffer(undefined)).toBe('');
    });

    it('возвращает пустую строку, если хоть один id - это пустая строка', () => {
        const mockOffer = createMockOffer({
            configurationId: '',
            complectationId: '2',
            techId: '3',
        });

        expect(getComplectationValueFromOffer(mockOffer)).toBe('');
    });
});

interface Args {
    configurationId: string;
    complectationId: string;
    techId: string;
}

function createMockOffer({ complectationId, configurationId, techId }: Args) {
    return {
        car_info: {
            configuration: { id: configurationId },
            complectation: { id: complectationId },
            tech_param: { id: techId },
        },
    } as RawOffer;
}
