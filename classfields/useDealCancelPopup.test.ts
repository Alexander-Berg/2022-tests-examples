import { act, renderHook } from '@testing-library/react-hooks';

import { CancellationReason, ParticipantType } from '@vertis/schema-registry/ts-types-snake/vertis/safe_deal/common';

import { CANCEL_REASONS } from 'auto-core/data/deal/cancel_reasons';

import type { Props } from './useDealCancelPopup';
import useDealCancelPopup from './useDealCancelPopup';

const updateDeal = jest.fn();
updateDeal.mockImplementation(() => Promise.resolve());
const closeDealCancelPopup = jest.fn();

const defaultProps: Props = {
    updateDeal,
    closeDealCancelPopup,
    cancelParty: ParticipantType.SELLER,
    dealId: '123',
};

it('isInputShown должен возвращать true', () => {
    const result = render().result.current;
    expect(result.isInputShown(CancellationReason.SELLER_ANOTHER_REASON)).toBe(true);
    expect(result.isInputShown(CancellationReason.BUYER_ANOTHER_REASON)).toBe(true);
});

describe('cancelDeal', () => {
    it('должен выставить cancelReasonInputError в true если выбрана другая причина и поле ввода причины пустое', () => {
        const { result } = render();

        act(() => {
            result.current.cancelDeal(CancellationReason.SELLER_ANOTHER_REASON);
        });
        expect(result.current.cancelReasonInputError).toBe(true);

        act(() => {
            result.current.cancelDeal(CancellationReason.BUYER_ANOTHER_REASON);
        });
        expect(result.current.cancelReasonInputError).toBe(true);
    });

    it('не должен вызывать updateDeal если cancelParty или dealId пустые', () => {
        const { result } = render({
            ...defaultProps,
            cancelParty: undefined,
            dealId: undefined,
        });

        result.current.cancelDeal(CancellationReason.SELLER_BECAUSE_INACTIVE_BUYER);

        expect(updateDeal).toHaveBeenCalledTimes(0);
    });

    describe('должен вызвать updateDeal', () => {
        it('cancelParty: BUYER и причина не другое', () => {
            const { result } = render({
                ...defaultProps,
                cancelParty: ParticipantType.BUYER,
            });

            result.current.cancelDeal(CancellationReason.BUYER_DEAL_WITH_ANOTHER_SELLER);

            expect(updateDeal).toHaveBeenCalledWith({
                data: {
                    by_buyer: {
                        cancel_deal_with_reason: {
                            cancel_description: '',
                            cancel_reason: 'BUYER_DEAL_WITH_ANOTHER_SELLER',
                        },
                    },
                }, deal_id: '123',
                message: 'Спасибо! Вы помогаете стать Авто.ру ещё лучше',
            });
        });

        it('cancelParty: SELLER и причина не другое', () => {
            const { result } = render({
                ...defaultProps,
                cancelParty: ParticipantType.SELLER,
            });

            result.current.cancelDeal(CancellationReason.SELLER_BECAUSE_INACTIVE_BUYER);

            expect(updateDeal).toHaveBeenCalledWith({
                data: {
                    by_seller: {
                        cancel_deal_with_reason: {
                            cancel_description: '',
                            cancel_reason: 'SELLER_BECAUSE_INACTIVE_BUYER',
                        },
                    },
                }, deal_id: '123',
                message: 'Спасибо! Вы помогаете стать Авто.ру ещё лучше',
            });
        });

        it('причина - другое', () => {
            const { result } = render({
                ...defaultProps,
                cancelParty: ParticipantType.SELLER,
            });

            act(() => {
                result.current.onCancelReasonInputChange('Вложил деньги в финансовую пирамиду и всё потерял');
            });

            result.current.cancelDeal(CancellationReason.SELLER_ANOTHER_REASON);

            expect(updateDeal).toHaveBeenCalledWith({
                data: {
                    by_seller: {
                        cancel_deal_with_reason: {
                            cancel_description: 'Вложил деньги в финансовую пирамиду и всё потерял',
                            cancel_reason: 'SELLER_ANOTHER_REASON',
                        },
                    },
                }, deal_id: '123',
                message: 'Спасибо! Вы помогаете стать Авто.ру ещё лучше',
            });
        });
    });
});

it('должен очищать состояния cancelReasonInput и cancelReasonInputError если dealId: undefined', () => {
    const { rerender, result } = render();

    act(() => {
        result.current.cancelDeal(CancellationReason.SELLER_ANOTHER_REASON);
    });
    expect(result.current.cancelReasonInputError).toBe(true);
    act(() => {
        result.current.onCancelReasonInputChange('прива');
    });
    expect(result.current.cancelReasonInput).toBe('прива');
    defaultProps.dealId = undefined;
    rerender();
    expect(result.current.cancelReasonInputError).toBe(false);
    expect(result.current.cancelReasonInput).toBe('');
});

describe('причины', () => {
    it('должен отдавать массив для продавца', () => {
        const { result } = render();

        expect(result.current.reasons).toEqual(CANCEL_REASONS.SELLER);
    });

    it('должен отдавать массив для покупателя', () => {
        const { result } = render({
            ...defaultProps,
            cancelParty: ParticipantType.BUYER,
        });

        expect(result.current.reasons).toEqual(CANCEL_REASONS.BUYER);
    });
});

function render(props = defaultProps) {
    return renderHook(() => useDealCancelPopup(props));
}
