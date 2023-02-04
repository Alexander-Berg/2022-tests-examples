import React from 'react';

import { InitialDataAction } from 'common/actions';
import { Page, PageProps } from 'common/__tests__/common.page';

import { ListAction } from '../constants';
import { reducers } from '../reducers';
import { rootSaga } from '../sagas';
import { RootContainer } from '../containers/RootContainer';

window.React = React;

export class ContractsPage extends Page {
    static selector = '.yb-contracts-search';

    constructor(props: PageProps) {
        super({ ...props, rootSaga, reducers, RootContainer });
    }

    async initializePage() {
        await this.sagaTester.waitFor(InitialDataAction.RECEIVE);
        this.wrapper.update();
    }

    async submitFilter() {
        const submitButton = this.wrapper.find('SearchFilter').find('[type="submit"]').at(1);

        submitButton.simulate('submit');

        await this.sagaTester.waitFor(ListAction.RECEIVE, true);
        this.wrapper.update();
    }

    getFilterValues() {
        return {
            contractEid: this.findElement('contract-eid', 'input').prop('value'),
            contractEidLike: this.findElement('contract-eid-like', 'input').prop<boolean>('value'),
            contractType: this.findElement('contract-type', 'Select').prop('value'),
            serviceId: this.findElement('service-id', 'Select').prop('value'),
            paymentType: this.findElement('payment-type', 'Select').prop('value'),
            dtType: this.findElement('dt-type', 'Select').prop('value'),
            dtFrom: this.findElement('dt-from', 'DatePicker').prop('date'),
            dtTo: this.findElement('dt-to', 'DatePicker').prop('date')
        };
    }
}
