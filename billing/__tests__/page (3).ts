import { Page, PageProps } from 'common/__tests__/common.page';

import { rootSaga } from '../sagas';
import { reducers } from '../reducers';
import { RootContainer } from '../containers/RootContainer';
import { ContractActions } from '../actions';

export class ContractPage extends Page {
    constructor(props: PageProps) {
        super({ ...props, rootSaga, reducers, RootContainer });
    }

    async waitForLoad() {
        await this.sagaTester.waitFor(ContractActions.RECEIVE_COLLATERALS);
        await this.sagaTester.waitFor(ContractActions.RECEIVE);
        await this.sagaTester.waitFor(ContractActions.RECEIVE_EXPORT_STATE);
    }
}
