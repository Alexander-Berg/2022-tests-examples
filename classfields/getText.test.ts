import type { Card } from 'auto-core/types/proto/auto/api/vin/garage/garage_api_model';

import getText from './getText';

const CARD_DRAFT = {
    vehicle_info: {
        car_info: {
            mark_info: { name: 'Lada' },
            model_info: { name: 'Granta' },
        },
    },
} as unknown as Card;

it('вернет человеческое название тачки', () => {
    const result = getText({
        cardDraft: CARD_DRAFT,
        status: 'Success',
    });

    expect(result).toBe('Автомобиль Lada Granta успешно добавлен');
});

it('вернет человеческое название тачки, status: Idle', () => {
    const result = getText({
        cardDraft: CARD_DRAFT,
        status: 'Idle',
    });

    expect(result).toBe('Добавь в Гараж мечты и узнай про неё больше!');
});

it('вернет человеческое название тачки, короткий текст, status: Idle', () => {
    const result = getText({
        cardDraft: CARD_DRAFT,
        status: 'Idle',
        'short': true,
    });

    expect(result).toBe('Добавь в Гараж мечты!');
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

    const result = getText({
        cardDraft,
        status: 'Success',
    });

    expect(result).toBe('Автомобиль LADA GRANTA успешно добавлен');
});

it('текст ошибки', () => {
    const result = getText({
        cardDraft: CARD_DRAFT,
        status: 'Error',
    });

    expect(result).toBe('Не удалось добавить машину в Гараж, попробуйте ещё раз');
});
