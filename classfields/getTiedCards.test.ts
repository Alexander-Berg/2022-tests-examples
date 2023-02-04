import type { BillingTiedCard } from '@vertis/schema-registry/ts-types-snake/auto/api/billing';

import type { StateUser, StateUserData } from 'auto-core/react/dataDomain/user/types';
import getTiedCards from 'auto-core/react/dataDomain/user/selectors/getTiedCards';

it('должен вернуть карты и отсортировать их по prefered', () => {
    const state: { user: StateUser } = {
        user: {
            data: {
                tied_cards: [
                    { id: '1', preferred: false } as BillingTiedCard,
                    { id: '2', preferred: true } as BillingTiedCard,
                    { id: '3', preferred: false } as BillingTiedCard,
                    { id: '4', preferred: true } as BillingTiedCard,
                ],
            } as StateUserData,
            pending: false,
        },
    };
    expect(getTiedCards(state)).toEqual([
        { id: '2', preferred: true } as BillingTiedCard,
        { id: '4', preferred: true } as BillingTiedCard,
        { id: '1', preferred: false } as BillingTiedCard,
        { id: '3', preferred: false } as BillingTiedCard,
    ]);
});

it('должен вернуть пустой масив, если карт нет', () => {
    const state: { user: StateUser } = {
        user: {
            data: {} as StateUserData,
            pending: false,
        },
    };
    expect(getTiedCards(state)).toEqual([]);
});
