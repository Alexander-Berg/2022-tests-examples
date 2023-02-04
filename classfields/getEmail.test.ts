import type { StateUser, StateUserData } from 'auto-core/react/dataDomain/user/types';
import getEmail from 'auto-core/react/dataDomain/user/selectors/getEmail';

it('должен вернуть первый подтвержденный email', () => {
    const state: { user: StateUser } = {
        user: {
            data: {
                emails: [
                    { email: '1', confirmed: false },
                    { email: '2', confirmed: true },
                ],
            } as StateUserData,
            pending: false,
        },
    };
    expect(getEmail(state)).toEqual('2');
});

it('должен вернуть undefined, если нет emailов', () => {
    const state: { user: StateUser } = {
        user: {
            data: {} as StateUserData,
            pending: false,
        },
    };
    expect(getEmail(state)).toBeUndefined();
});

it('должен вернуть undefined, если нет подтвержденных emailов', () => {
    const state: { user: StateUser } = {
        user: {
            data: {
                emails: [
                    { email: '1', confirmed: false },
                    { email: '2', confirmed: false },
                ],
            } as StateUserData,
            pending: false,
        },
    };
    expect(getEmail(state)).toBeUndefined();
});
