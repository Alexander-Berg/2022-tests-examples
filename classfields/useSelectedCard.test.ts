import { renderHook, act } from '@testing-library/react-hooks';

import { useLocalStorage } from 'auto-core/react/hooks/react-use';

import type { Card } from 'auto-core/types/proto/auto/api/vin/garage/garage_api_model';

import useSelectedCard, { LS_KEY_SELECTED_CARD } from './useSelectedCard';

const STORED_VALUE = '1111111';
const DEFAULT_VALUE = '';
const FIRST_CARD_ID = '12345';

beforeEach(() => {
    const { result } = renderHook(() => useLocalStorage(LS_KEY_SELECTED_CARD as string, DEFAULT_VALUE));
    const setLsDefaultCard = result.current[1];
    act(() => {
        setLsDefaultCard(STORED_VALUE);
    });
});

it('установит значение из локал стораджа', () => {
    const { result } = renderHook(() => useSelectedCard(DEFAULT_VALUE));
    expect(result.current.selectedCard).toBe(STORED_VALUE);
});

it('сохранит значение, если нет массива', () => {
    const { result } = renderHook(() => useSelectedCard(DEFAULT_VALUE));

    act(() => {
        // Просто на всякий проверим, не сломается ли
        result.current.resetSelectedCard(undefined as unknown as Array<Card>);
    });

    expect(result.current.selectedCard).toBe(STORED_VALUE);
});

it('сохранит значение, если ноль карточек', () => {
    const { result } = renderHook(() => useSelectedCard(DEFAULT_VALUE));

    act(() => {
        result.current.resetSelectedCard([]);
    });

    expect(result.current.selectedCard).toBe(STORED_VALUE);
});

it('сбросит до первой, если карточки есть, но нет текущего значения', () => {
    const { result } = renderHook(() => useSelectedCard(DEFAULT_VALUE));

    act(() => {
        result.current.resetSelectedCard([
            { id: FIRST_CARD_ID },
            { id: 'efsefsefsef' },
        ] as Array<Card>);
    });

    expect(result.current.selectedCard).toBe(FIRST_CARD_ID);
});

it('оставит текущую в общем случае', () => {
    const { result } = renderHook(() => useSelectedCard(DEFAULT_VALUE));

    act(() => {
        result.current.resetSelectedCard([
            { id: FIRST_CARD_ID },
            { id: 'efsefsefsef' },
            { id: STORED_VALUE },
        ] as Array<Card>);
    });

    expect(result.current.selectedCard).toBe(STORED_VALUE);
});
