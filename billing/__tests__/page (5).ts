import React from 'react';

import { Page, PageProps } from 'common/__tests__/common.page';
import { InitialDataAction } from 'common/actions';

import reducers from '../reducers';
import rootSaga from '../sagas';
import RootContainer from '../containers/RootContainer';

window.React = React;

export class InvoicesPage extends Page {
    static selector = '.yb-invoices-search';

    constructor(props: PageProps) {
        super({ ...props, rootSaga, reducers, RootContainer });
    }

    async initializePage() {
        await this.sagaTester.waitFor(InitialDataAction.RECEIVE);
        this.wrapper.update();
    }

    getFilterValues() {
        return {
            dateType: this.findElement('date-type', 'Select').prop('value'),
            dateFrom: this.findElement('date-from', 'DatePicker').prop('date'),
            dateTo: this.findElement('date-to', 'DatePicker').prop('date'),
            invoiceEid: this.findElement('invoice-eid', 'input').prop('value'),
            paymentStatus: this.findElement('payment-status', 'Select').prop('value'),
            firm: this.findElement('firm', 'Select').prop('value'),
            postPayType: this.findElement('post-pay-type', 'Select').prop('value'),
            troubleType: this.findElement('trouble-type', 'Select').prop('value'),
            service: this.findElement('service', 'Select').prop('value'),
            serviceOrderId: this.findElement('service-order-id', 'input').prop('value'),
            contractEid: this.findElement('contract-eid', 'input').prop('value')
        };
    }
}
