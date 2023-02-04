import invoice from '../invoice';
import { INVOICE_INFO } from '../../actions';

describe('admin - invoice - reducers - invoice', () => {
    it('payment refund amount change to less then available - success', () => {
        expect.assertions(1);

        const state = {
            oebs: {
                payments1c: {
                    items: [
                        {
                            cpfId: 100,
                            refundableAmount: '100',
                            __orig: {
                                cpfId: 100,
                                refundableAmount: '100'
                            }
                        }
                    ]
                }
            }
        };

        const nextState = invoice(state, {
            type: INVOICE_INFO.PAYMENT_CHANGE,
            cpfId: 100,
            req: 'refundableAmount',
            val: '50'
        });

        expect(nextState.oebs.payments1c.items[0].refundableAmount).toBe('50');
    });

    it('payment refund amount change to more then available - validation error', () => {
        expect.assertions(1);

        const state = {
            oebs: {
                payments1c: {
                    items: [
                        {
                            cpfId: 100,
                            refundableAmount: '100',
                            __orig: {
                                cpfId: 100,
                                refundableAmount: '100'
                            }
                        }
                    ]
                }
            }
        };

        const nextState = invoice(state, {
            type: INVOICE_INFO.PAYMENT_CHANGE,
            cpfId: 100,
            req: 'refundableAmount',
            val: '150'
        });

        expect(nextState.oebs.payments1c.items[0].refundErrors.refundableAmount).toBe(
            'unavailableAmount'
        );
    });

    it('payment refund requisite change', () => {
        expect.assertions(1);

        const state = {
            oebs: {
                payments1c: {
                    items: [
                        {
                            editableRefundRequisites: ['customerName'],
                            cpfId: 100,
                            refundRequisites: {
                                account: '111111111111111',
                                bik: '123456',
                                inn: '789123807369',
                                customerName: 'Иван'
                            }
                        }
                    ]
                }
            }
        };

        const nextState = invoice(state, {
            type: INVOICE_INFO.PAYMENT_CHANGE,
            cpfId: 100,
            req: 'customerName',
            val: 'Петр'
        });

        expect(nextState.oebs.payments1c.items[0].refundRequisites.customerName).toBe('Петр');
    });
});
