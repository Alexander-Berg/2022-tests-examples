import { Page, PageProps } from 'common/__tests__/common.page';
import { InitialDataAction } from 'common/actions';

import { rootSaga } from '../sagas';
import { reducers } from '../reducers';
import { RootContainer } from '../containers/RootContainer';
import { FilterAction, ListAction } from '../actions';

export class OrdersPage extends Page {
    static selector = '.yb-orders-search';

    constructor(props: PageProps) {
        super({ ...props, rootSaga, reducers, RootContainer });
    }

    async initializePage() {
        await this.sagaTester.waitFor(InitialDataAction.RECEIVE);
        this.wrapper.update();
    }

    async initializePageFromHistory() {
        await this.sagaTester.waitFor(FilterAction.APPLY);
        this.wrapper.update();
    }

    async submitFilter() {
        super.submitFilter();

        await this.sagaTester.waitFor(ListAction.RECEIVE, true);
        this.wrapper.update();
    }

    getFilterValues() {
        return {
            dtFrom: this.findElement('dt-from', 'DatePicker').prop('date'),
            dtTo: this.findElement('dt-to', 'DatePicker').prop('date'),
            agency: this.findElement('agency', 'ModalSelector').prop('text'),
            client: this.findElement('client', 'ModalSelector').prop('text'),
            serviceOrderId: this.findElement('service-order-id', 'input').prop('value'),
            paymentStatus: this.findElement('payment-status', 'Select').prop('value'),
            service: this.findElement('service', 'Select').prop('value')
        };
    }
}
