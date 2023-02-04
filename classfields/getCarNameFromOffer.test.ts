import type { Offer } from 'auto-core/types/proto/auto/api/api_offer_model';

import { getCarNameFromOffer } from './getCarNameFromOffer';

describe('getCarNameFromOffer', () => {
    it('возвращает строку, состоящую из марки, модели, поколения и года', () => {
        const mockOffer = {
            vehicle_info: {
                mark_info: { name: 'BMW' },
                model_info: { name: '3 серия' },
                super_gen: { id: '1', name: 'VI (F3x)' },
            },
            documents: {
                year: 2017,
            },
        } as Offer;

        expect(getCarNameFromOffer(mockOffer)).toBe('BMW 3 серия VI (F3x), 2017');
    });

    it('не добавляет запятую и год, если года нет', () => {
        const mockOffer = {
            vehicle_info: {
                mark_info: { name: 'BMW' },
                model_info: { name: '3 серия' },
                super_gen: { id: '1', name: 'VI (F3x)' },
            },
        } as Offer;

        expect(getCarNameFromOffer(mockOffer)).toBe('BMW 3 серия VI (F3x)');
    });
});
