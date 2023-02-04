import React from 'react';

import { InitialDataAction } from 'common/actions';
import { Page, PageProps } from 'common/__tests__/common.page';

import { reducers } from '../reducers';
import { rootSaga } from '../sagas';
import { RootContainer } from '../containers/RootContainer';
import { ListAction } from '../types';

window.React = React;

export class PaymentsPage extends Page {
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

    getListItems() {
        return this.wrapper.find('.yb-payments-table tbody tr');
    }
}
