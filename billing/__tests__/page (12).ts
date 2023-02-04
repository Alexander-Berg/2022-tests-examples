import { Page, PageProps } from 'common/__tests__/common.page';
import { InitialDataAction } from 'common/actions';

import { rootSaga } from '../sagas';
import { reducers } from '../reducers';
import { RootContainer } from '../containers/RootContainer';
import { ListAction } from '../actions';

export class RequestsPage extends Page {
    static selector = '.yb-requests-search';

    constructor(props: PageProps) {
        super({ ...props, rootSaga, reducers, RootContainer });
    }

    async initializePage() {
        await this.sagaTester.waitFor(InitialDataAction.RECEIVE);
        this.wrapper.update();
    }

    async submitFilter() {
        super.submitFilter();

        await this.sagaTester.waitFor(ListAction.RECEIVE, true);
        this.wrapper.update();
    }
}
