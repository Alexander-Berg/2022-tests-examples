import { CardBrand, PaymentSystemId } from '@vertis/schema-registry/ts-types-snake/vertis/banker/api_model';
import { SocialProvider } from '@vertis/schema-registry/ts-types-snake/vertis/common';

import type { DeleteSocialProfilesAction, StateUser, TiedCardUpdateAction } from './types';
import { USER_DELETE_SOCIAL_PROFILE, USER_TIED_CARD_UPDATE } from './types';
import userMock from './mocks';
import reducer from './reducer';

let state: StateUser;
beforeEach(() => {
    state = {
        data: {
            id: '25752920',
            auth: true,
            name: 'id25752920',
            tied_cards: [
                {
                    id: '444444448',
                    card_mask: '4444444448',
                    preferred: false,
                    ps_id: PaymentSystemId.YANDEXKASSA,
                    properties: {
                        cdd_pan_mask: '444444|4448',
                        brand: CardBrand.VISA,
                        expire_year: '2021',
                        expire_month: '12',
                        verification_required: true,
                        invoice_id: '',
                        card_bank: 'Tinkoff',
                    },
                },
                {
                    id: '555554444',
                    card_mask: '5555554444',
                    ps_id: PaymentSystemId.YANDEXKASSA_V3,
                    preferred: false,
                    properties: {
                        cdd_pan_mask: '555555|4444',
                        brand: CardBrand.MASTERCARD,
                        expire_year: '2021',
                        expire_month: '12',
                        verification_required: true,
                        invoice_id: '',
                        card_bank: 'Tinkoff',
                    },
                },
            ],
            balance: 7,
            user_balance: '7',
            isModerator: false,
        },
        pending: false,
    };
});

describe('USER_TIED_CARD_UPDATE', () => {
    it('должен обновить карту из payload', () => {
        const action: TiedCardUpdateAction = {
            type: USER_TIED_CARD_UPDATE,
            payload: [
                {
                    id: '555554444',
                    card_mask: '5555554444',
                    ps_id: PaymentSystemId.YANDEXKASSA_V3,
                    preferred: true,
                    properties: {
                        cdd_pan_mask: '555555|4444',
                        brand: CardBrand.MASTERCARD,
                        expire_year: '2022',
                        expire_month: '12',
                        verification_required: false,
                        invoice_id: '',
                        card_bank: 'Tinkoff',
                    },
                },
            ],
        };

        expect(reducer(state, action)).toMatchObject({
            data: {
                tied_cards: [
                    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
                    // @ts-ignore
                    state.data.tied_cards[0],
                    action.payload[0],
                ],
            },
        });
    });

    it('не должен обновлять ненайденные карты', () => {
        const action: TiedCardUpdateAction = {
            type: USER_TIED_CARD_UPDATE,
            payload: [
                {
                    id: '12345',
                    card_mask: '12345',
                    ps_id: PaymentSystemId.YANDEXKASSA_V3,
                    preferred: true,
                },
            ],
        };

        expect(reducer(state, action).data.tied_cards).toEqual(state.data.tied_cards);
    });
});

describe('USER_DELETE_SOCIAL_PROFILE', () => {
    it('должен удалить социальный профиль', () => {
        const user = userMock.withAuth(true).withSocialProfiles([
            {
                provider: SocialProvider.MAILRU,
                social_user_id: '6013748787654302824',
                added: '2018-05-31T14:24:20Z',
                nickname: 'natix',
                first_name: 'Игорь',
                last_name: 'Стуев',
                trusted: false,
            },
            {
                provider: SocialProvider.YANDEX,
                social_user_id: '1130000021057444',
                added: '2019-05-13T11:05:13Z',
                nickname: 'nickname1',
                first_name: 'first_name1',
                last_name: 'last_name1',
                trusted: false,
            },
            {
                provider: SocialProvider.YANDEX,
                social_user_id: '84273938',
                added: '2019-05-13T11:05:48Z',
                nickname: 'nickname2',
                first_name: 'first_name2',
                last_name: 'last_name2',
                trusted: false,
            },
        ]);

        const action: DeleteSocialProfilesAction = {
            type: USER_DELETE_SOCIAL_PROFILE,
            payload: {
                provider: SocialProvider.YANDEX,
                socialUserId: '84273938',
            },
        };

        expect(reducer(user.value(), action).data.social_profiles).toEqual([
            {
                provider: SocialProvider.MAILRU,
                social_user_id: '6013748787654302824',
                added: '2018-05-31T14:24:20Z',
                nickname: 'natix',
                first_name: 'Игорь',
                last_name: 'Стуев',
                trusted: false,
            },
            {
                provider: SocialProvider.YANDEX,
                social_user_id: '1130000021057444',
                added: '2019-05-13T11:05:13Z',
                nickname: 'nickname1',
                first_name: 'first_name1',
                last_name: 'last_name1',
                trusted: false,
            },
        ]);
    });
});
