import { statePaymentStatusToQS, qSPaymentStatusToState } from '../payment-status';
import { PaymentStatus } from '../../constants';

describe('tests for conveting payment status utils', () => {
    it('statePaymentStatusToQS', () => {
        expect(statePaymentStatusToQS(PaymentStatus.UNDEFINED)).toBe(0);
        expect(statePaymentStatusToQS(PaymentStatus.ALL_ORDERS)).toBe(1);
        expect(statePaymentStatusToQS(PaymentStatus.WITHOUT_INVOICE)).toBe(2);
        expect(statePaymentStatusToQS(PaymentStatus.WITH_REQUEST)).toBe(3);
        expect(statePaymentStatusToQS(PaymentStatus.UNPAID_INVOICE)).toBe(4);
        expect(statePaymentStatusToQS(PaymentStatus.UNPAID_ORDERS)).toBe(5);
        expect(statePaymentStatusToQS(PaymentStatus.PAID_INVOICE)).toBe(6);
    });

    it('qSPaymentStatusToState', () => {
        expect(qSPaymentStatusToState(0)).toBe(PaymentStatus.UNDEFINED);
        expect(qSPaymentStatusToState(1)).toBe(PaymentStatus.ALL_ORDERS);
        expect(qSPaymentStatusToState(2)).toBe(PaymentStatus.WITHOUT_INVOICE);
        expect(qSPaymentStatusToState(3)).toBe(PaymentStatus.WITH_REQUEST);
        expect(qSPaymentStatusToState(4)).toBe(PaymentStatus.UNPAID_INVOICE);
        expect(qSPaymentStatusToState(5)).toBe(PaymentStatus.UNPAID_ORDERS);
        expect(qSPaymentStatusToState(6)).toBe(PaymentStatus.PAID_INVOICE);
    });
});
