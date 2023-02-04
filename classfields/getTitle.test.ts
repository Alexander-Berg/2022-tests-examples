import type { Card } from 'auto-core/types/proto/auto/api/vin/garage/garage_api_model';

import getTitle from './getTitle';

const CARD_DRAFT = {
    vehicle_info: {
        car_info: {
            mark_info: { name: 'Lada' },
            model_info: { name: 'Granta' },
        },
    },
} as unknown as Card;

it('вернет undefined, если статус не Idle', () => {
    const result = getTitle({
        cardDraft: CARD_DRAFT,
        status: 'Success',
    });

    expect(result).toBe(undefined);
});

it('вернет человеческое название тачки, status: Idle', () => {
    const result = getTitle({
        cardDraft: CARD_DRAFT,
        status: 'Idle',
    });

    expect(result).toBe('Нравится Lada Granta?');
});

it('вернет нечеловеческое название тачки', () => {
    const cardDraft = {
        vehicle_info: {
            car_info: {
                mark: 'LADA',
                model: 'GRANTA',
            },
        },
    } as unknown as Card;

    const result = getTitle({
        cardDraft,
        status: 'Idle',
    });

    expect(result).toBe('Нравится LADA GRANTA?');
});
