import React from 'react';

import { InitialDataAction } from 'common/actions';
import { Page, PageProps } from 'common/__tests__/common.page';

import { reducers } from '../reducers';
import { rootSaga } from '../sagas';
import { RootContainer } from '../containers/RootContainer';
import { ListAction } from '../actions';

window.React = React;

export class DeferpaysPage extends Page {
    static selector = '.yb-deferpays-search';

    constructor(props: PageProps) {
        super({ ...props, rootSaga, reducers, RootContainer });
    }

    async initializePage() {
        await this.sagaTester.waitFor(InitialDataAction.RECEIVE);
        await this.sagaTester.waitFor(ListAction.RECEIVE, true);
        this.wrapper.update();
    }

    async submitFilter() {
        super.submitFilter();

        await this.sagaTester.waitFor(ListAction.RECEIVE, true);
        this.wrapper.update();
    }

    fillCheckbox(deferpayId: number) {
        // @ts-ignore
        this.getCheckbox(deferpayId).prop('onChange')({ target: { checked: true } });

        // await this.sagaTester.waitFor(ListAction.SWITCH_ORDER);
        this.wrapper.update();
    }

    fillActionName(type: string) {
        // @ts-ignore
        this.wrapper
            .find('.yb-deferpays-action__action Select')
            // @ts-ignore
            .prop('onChange')(type);
        this.wrapper.update();
    }

    fillActionDate(date: string) {
        // @ts-ignore
        this.wrapper
            .find('.yb-deferpays-action__datepicker DatePicker')
            // @ts-ignore
            .prop('onChange')(date);
    }

    submitActionForm() {
        // @ts-ignore
        this.wrapper
            .find('#deferpays-action-form')
            // @ts-ignore
            .prop('onSubmit')({ preventDefault: () => {} });
    }

    getCheckbox(deferpayId: number) {
        return this.wrapper
            .find(`.yb-deferpays-table__checkbox-${deferpayId} input.Checkbox-Control`)
            .at(0);
    }

    getPersonLink() {
        return this.wrapper.find('.yb-deferpays-table__person a');
    }

    getFilterValues() {
        return {
            agency: this.findElement('agency', 'input').prop('value'),
            paymentStatus: this.findElement('payment-status', 'Select').prop('value'),
            service: this.findElement('service', 'Select').prop('value'),
            contract: this.findElement('contract', 'Select').prop('value'),
            orderId: this.findElement('order-id', 'input').prop('value'),
            dateFrom: this.findElement('date-from', 'DatePicker').prop('date'),
            dateTo: this.findElement('date-to', 'DatePicker').prop('date')
        };
    }
}
