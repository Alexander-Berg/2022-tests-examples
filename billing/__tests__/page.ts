import { Page, PageProps } from 'common/__tests__/common.page';
import rootSaga from '../sagas';
import reducers from '../reducers';
import { RootContainer } from '../containers/RootContainer';
import { InitialDataAction } from 'common/actions';
import { CLIENT, LIST, MANAGER, PERSON } from '../actions';

export class ActsPage extends Page {
    static selector = '.yb-acts-search';

    constructor(props: PageProps) {
        super({ ...props, rootSaga, reducers, RootContainer });
    }

    async initializePage() {
        await this.sagaTester.waitFor(InitialDataAction.RECEIVE);
        await this.sagaTester.waitFor(LIST.RECEIVE);
        await this.sagaTester.waitFor(PERSON.RECEIVE);
        await this.sagaTester.waitFor(CLIENT.RECEIVE);
        await this.sagaTester.waitFor(MANAGER.RECEIVE);

        this.wrapper.update();
    }

    getFilterValues() {
        return {
            externalId: this.findElement('external-id', 'input').prop('value'),
            factura: this.findElement('factura', 'input').prop('value'),
            eid: this.findElement('invoice-eid', 'input').prop('value'),
            contractEid: this.findElement('contract-eid', 'input').prop('value'),
            service: this.findElement('service', 'Select').prop('value'),
            firm: this.findElement('firm', 'Select').prop('value'),
            dtFrom: this.findElement('act-dt-from', 'DatePicker').prop('date'),
            dtTo: this.findElement('act-dt-to', 'DatePicker').prop('date'),
            currencyCode: this.findElement('currency-code', 'Select').prop('value'),
            client: this.findElement('client', 'Textinput').at(0).prop('value'),
            person: this.findElement('person', 'Textinput').at(0).prop('value'),
            manager: this.findElement('manager', 'input').prop('value')
        };
    }
}
