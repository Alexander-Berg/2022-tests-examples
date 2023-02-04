import React from 'react';

import { InitialDataAction } from 'common/actions';
import { Page, PageProps } from 'common/__tests__/common.page';

import { reducers } from '../reducers';
import { rootSaga } from '../sagas';
import { Root } from '../components/Root';
import { ListActionType } from '../constants';

window.React = React;

export class PartnerContractsPage extends Page {
    constructor(props: PageProps) {
        super({ ...props, rootSaga, reducers, RootContainer: Root });
    }

    async initializePage() {
        await this.sagaTester.waitFor(InitialDataAction.RECEIVE);
        this.wrapper.update();
    }

    async submitFilter() {
        super.submitFilter();

        await this.sagaTester.waitFor(ListActionType.RECEIVE, true);
        this.wrapper.update();
    }

    getState() {
        return {
            contractClass: this.getFilterValueFromSelect('contract-class'),
            firmId: this.getFilterValueFromSelect('firm-id'),
            dtType: this.getFilterValueFromSelect('dt-type'),
            dtFrom: this.getFilterValueFromDatepicker('dt-from'),
            dtTo: this.getFilterValueFromDatepicker('dt-to'),
            contractEid: this.getFilterValueFromInput('contract-eid'),
            contractEidLike: this.getFilterValueFromInput('contract-eid-like'),
            offer: this.getFilterValueFromSelect('offer'),
            contractType: this.getFilterValueFromSelect('contract-type'),
            billInterval: this.getFilterValueFromSelect('bill-interval'),
            docSet: this.getFilterValueFromSelect('doc-set')
        };
    }

    private getFilterValueFromSelect(field: string) {
        const item = this.getField(field).find('Select');
        return item.length === 0 ? null : item.prop('value');
    }

    private getFilterValueFromDatepicker(field: string) {
        const item = this.getField(field).find('DatePicker');
        return item.length === 0 ? null : item.prop('date');
    }

    private getFilterValueFromInput(field: string) {
        const item = this.getField(field).find('input');
        return item.length === 0 ? null : item.prop('value');
    }

    private getField(field: string) {
        return this.wrapper.find(`.yb-partner-contracts-search__${field}`);
    }
}
