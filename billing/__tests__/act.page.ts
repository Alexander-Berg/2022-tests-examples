import { Page, PageProps } from 'common/__tests__/common.page';
import rootSaga from '../sagas';
import reducers from '../reducers';
import { RootContainer } from '../containers/RootContainer';

export class ActPage extends Page {
    constructor(props: PageProps) {
        super({ ...props, rootSaga, reducers, RootContainer });
    }

    async initializePage() {
        await this.sagaTester.waitFor('ACT__RECEIVE');
        this.wrapper.update();
    }
}
