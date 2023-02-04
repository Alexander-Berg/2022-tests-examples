import React from 'react';

import { InitialDataAction } from 'common/actions';
import { Page, PageProps } from 'common/__tests__/common.page';

import { reducers } from '../reducers';
import { rootSaga } from '../sagas';
import { RootContainer } from '../containers/RootContainer';
import { ListAction } from '../actions';

window.React = React;

export class PersonsPage extends Page {
    static selector = '.yb-persons-search';

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
            name: this.findElement('name', 'input').prop('value'),
            personType: this.findElement('person-type', 'Select').prop('value'),
            personId: this.findElement('person-id', 'input').prop('value'),
            inn: this.findElement('inn', 'input').prop('value'),
            email: this.findElement('email', 'input').prop('value'),
            isPartner: this.findElement('is-partner', 'Select').prop('value'),
            vipOnly: this.findElement('vip-only', 'input').prop('checked')
        };
    }
}
