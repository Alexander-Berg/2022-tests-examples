import React from 'react';

import { Page, PageProps } from 'common/__tests__/common.page';
import { ContractType } from 'common/constants/print-form-rules';

import { reducers } from '../reducers';
import { rootSaga } from '../sagas';
import { Root } from '../components/Root';
import { receiveList, attributeReferenceLoaded } from '../actions';

window.React = React;

export class PrintFormRulesPage extends Page {
    constructor(props: PageProps) {
        super({ ...props, rootSaga, reducers, RootContainer: Root });
    }

    async initializePage() {
        await this.sagaTester.waitFor(attributeReferenceLoaded.type);
        this.wrapper.update();
    }

    async initializePageFromHistory() {
        await this.sagaTester.waitFor(attributeReferenceLoaded.type);
        await this.sagaTester.waitFor(receiveList.type, true);
        this.wrapper.update();
    }

    async submitFilter() {
        super.submitFilter();

        await this.sagaTester.waitFor(receiveList.type, true);
        this.wrapper.update();
    }

    async changeContractType(contractType: ContractType) {
        this.fillSelect('Вид договора', contractType);
        await this.sagaTester.waitFor(attributeReferenceLoaded.type, true);
        this.wrapper.update();
    }

    getContractType() {
        return this.getSelect('Вид договора').prop('value');
    }
}
