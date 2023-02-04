import { Page, PageProps } from 'common/__tests__/common.page';
import { InitialDataAction } from 'common/actions';

import { rootSaga } from '../sagas';
import { reducers } from '../reducers';
import { RootContainer } from '../containers/RootContainer';
import { ListAction } from '../actions';

export class ClientsPage extends Page {
    static selector = '.yb-clients-search';

    constructor(props: PageProps) {
        super({ ...props, rootSaga, reducers, RootContainer });
    }

    async initializePage() {
        await this.sagaTester.waitFor(InitialDataAction.RECEIVE);
        await this.sagaTester.waitFor(ListAction.RECEIVE);
        this.wrapper.update();
    }

    getFilterValues() {
        return {
            // 1 столбец
            name: this.findElement('name', 'input').prop('value'),
            login: this.findElement('login', 'input').prop('value'),
            clientId: this.findElement('client-id', 'input').prop('value'),
            singleAccountNumber: this.findElement('single-account-number', 'input').prop('value'),
            agencySelectPolicy: this.findElement('agency-select-policy', 'Select').prop('value'),

            // 2 столбец
            url: this.findElement('url', 'input').prop('value'),
            email: this.findElement('email', 'input').prop('value'),
            phone: this.findElement('phone', 'input').prop('value'),
            fax: this.findElement('fax', 'input').prop('value'),
            withInvoices: this.findElement('with-invoices', 'Select').prop('value'),

            // 3 столбец
            manager: this.findElement('manager', 'input').prop('value'),
            intercompany: this.findElement('intercompany', 'Select').prop('value'),
            isAccurate: this.findElement('is-accurate', 'Checkbox').at(0).prop<boolean>('value'),
            hideManagers: this.findElement('hide-managers', 'Checkbox')
                .at(0)
                .prop<boolean>('value'),
            manualSuspect: this.findElement('manual-suspect', 'Checkbox')
                .at(0)
                .prop<boolean>('value'),
            reliableClient: this.findElement('reliable-client', 'Checkbox')
                .at(0)
                .prop<boolean>('value')
        };
    }
}
