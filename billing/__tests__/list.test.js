import { fromJS } from 'immutable';

import { ListAction } from '../../actions';
import list, { ListState } from '../list';

describe('admin - invoices - reducers - list', () => {
    it('confirm payment request', () => {
        expect.assertions(1);

        const state = ListState({
            items: fromJS([
                {
                    invoiceId: 1,
                    receiptDt: null,
                    manager: null
                },
                {
                    invoiceId: 2,
                    receiptDt: null,
                    manager: null
                },
                {
                    invoiceId: 3,
                    receiptDt: null,
                    manager: null
                }
            ])
        });

        const nextState = list(state, {
            type: ListAction.CONFIRM_PAYMENT_REQUEST,
            invoiceId: 2
        });

        const item = nextState.items.find(i => i.get('invoiceId') === 2);

        expect(item.get('isConfirmPaymentFetching')).toBeTruthy();
    });

    it('confirm payment receive', () => {
        expect.assertions(8);

        const state = ListState({
            items: fromJS([
                {
                    invoiceId: 1,
                    receiptDt: null,
                    receiptSum: null,
                    paysysInstant: 0,
                    manager: null,
                    canManualTurnOn: false,
                    suspect: '1',
                    paysysCc: 'aa'
                },
                {
                    invoiceId: 2,
                    receiptDt: null,
                    receiptSum: null,
                    paysysInstant: 0,
                    manager: null,
                    canManualTurnOn: false,
                    suspect: '1',
                    paysysCc: 'aa'
                },
                {
                    invoiceId: 3,
                    receiptDt: null,
                    receiptSum: null,
                    paysysInstant: 0,
                    manager: null,
                    canManualTurnOn: false,
                    suspect: '1',
                    paysysCc: 'aa'
                }
            ])
        });

        const nextState = list(state, {
            type: ListAction.CONFIRM_PAYMENT_RECEIVE,
            invoiceId: 2,
            receiptDt: '2019-03-31T00:00:00',
            receiptSum: '1000',
            paysysInstant: 1,
            manager: 'user',
            canManualTurnOn: true,
            suspect: '2',
            paysysCc: 'bb'
        });

        const item = nextState.items.find(i => i.get('invoiceId') === 2);

        expect(item.get('isConfirmPaymentFetching')).toBeFalsy();
        expect(item.get('receiptDt')).toBe('2019-03-31T00:00:00');
        expect(item.get('receiptSum')).toBe('1000');
        expect(item.get('paysysInstant')).toBe(1);
        expect(item.get('manager')).toBe('user');
        expect(item.get('canManualTurnOn')).toBeTruthy();
        expect(item.get('suspect')).toBe('2');
        expect(item.get('paysysCc')).toBe('bb');
    });
});
