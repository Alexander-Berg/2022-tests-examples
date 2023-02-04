import { Page, PageProps } from 'common/__tests__/common.page';
import { InitialDataAction } from 'common/actions';

import { rootSaga } from '../sagas';
import { reducers } from '../reducers';
import { RootContainer } from '../containers/RootContainer';
import { ListAction } from '../types';

export class ProductCatalogPage extends Page {
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
