import React from 'react';

import { reducers } from '../reducers';
import { Page, PageProps } from 'common/__tests__/common.page';
import { rootSaga } from '../sagas';
import { Root } from '../components/Root';
import { SendContractsActionType, ListActionType, FilterActionType } from '../constants';

window.React = React;

export class PdfsendPage extends Page {
    constructor(props: PageProps) {
        super({ ...props, rootSaga, reducers, RootContainer: Root });
    }

    async initializePage() {
        await this.sagaTester.waitFor(FilterActionType.INITIALIZED);
        this.wrapper.update();
    }

    async submitFilter() {
        super.submitFilter();

        await this.sagaTester.waitFor(ListActionType.RECEIVE, true);
        this.wrapper.update();
    }

    checkItem() {
        const checkboxInput = this.wrapper
            .find('SearchList')
            .find('tbody')
            .find('tr')
            .find('input[type="checkbox"]')
            .at(0);

        checkboxInput.simulate('change', { target: { checked: true } });
    }

    async openSendModal() {
        const openSendModalButton = this.wrapper.find('SearchList').find('[type="button"]').at(0);

        openSendModalButton.simulate('click');

        await this.sagaTester.waitFor(SendContractsActionType.INITIALIZED, true);
        this.wrapper.update();
    }

    private async submitSendForm(actionToWait: string) {
        const sendButton = this.wrapper.find('SendContractsForm').find('button').at(1);

        sendButton.simulate('submit');

        await this.sagaTester.waitFor(actionToWait);
        this.wrapper.update();
    }

    submitSendFormWithSuccess() {
        return this.submitSendForm(SendContractsActionType.SEND_SUCCESS);
    }

    submitSendFormWithFailure() {
        return this.submitSendForm(SendContractsActionType.SEND_ERROR);
    }

    getSendFormMessage() {
        return this.wrapper
            .find('SendContractsForm')
            .find('.yb-send-contracts-form__message')
            .text();
    }
}
