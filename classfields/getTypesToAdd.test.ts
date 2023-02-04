import { CardTypeInfo_CardType } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/garage/garage_api_model';

import type { Card } from 'auto-core/types/proto/auto/api/vin/garage/garage_api_model';

import getTypesToAdd, { TYPES } from './getTypesToAdd';

it('должен вернуть все три типа, когда нет карточек', () => {
    expect(getTypesToAdd([])).toEqual(TYPES);
});

it('должен вернуть два типа dream + ex, когда есть current', () => {
    expect(getTypesToAdd([
        { card_type_info: { card_type: CardTypeInfo_CardType.CURRENT_CAR } } as unknown as Card,
    ])).toEqual([ CardTypeInfo_CardType.DREAM_CAR, CardTypeInfo_CardType.EX_CAR ]);
});

it('должен вернуть два типа current + ex, когда есть dream', () => {
    expect(getTypesToAdd([
        { card_type_info: { card_type: CardTypeInfo_CardType.DREAM_CAR } } as unknown as Card,
    ])).toEqual([ CardTypeInfo_CardType.CURRENT_CAR, CardTypeInfo_CardType.EX_CAR ]);
});

it('должен вернуть два типа ex, когда есть current + dream', () => {
    expect(getTypesToAdd([
        { card_type_info: { card_type: CardTypeInfo_CardType.CURRENT_CAR } } as unknown as Card,
        { card_type_info: { card_type: CardTypeInfo_CardType.DREAM_CAR } } as unknown as Card,
    ])).toEqual([ CardTypeInfo_CardType.EX_CAR ]);
});

it('должен вернуть пустой массив, когда все типы уже есть', () => {
    expect(getTypesToAdd([
        { card_type_info: { card_type: CardTypeInfo_CardType.CURRENT_CAR } } as unknown as Card,
        { card_type_info: { card_type: CardTypeInfo_CardType.DREAM_CAR } } as unknown as Card,
        { card_type_info: { card_type: CardTypeInfo_CardType.EX_CAR } } as unknown as Card,
    ])).toEqual([]);
});
