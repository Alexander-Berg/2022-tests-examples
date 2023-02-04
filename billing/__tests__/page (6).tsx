import React from 'react';
import { ReactWrapper } from 'enzyme';

import { Page, PageProps } from 'common/__tests__/common.page';
import { MessagesContainer } from 'common/containers/MessagesContainer';

import { getRootModule } from '../module';
import { RootContainer } from '../containers/RootContainer';
import {
    createdRequest,
    failedToCreateRequest,
    failedToProcessRequest,
    initializedPage,
    loadedRequests,
    processedRequest
} from '../actions';

interface RequestParams {
    firmId: string;
    contractId?: string;
    personId: string;
    periodStart: string;
    periodEnd: string;
}

export abstract class ReconciliationsPage extends Page {
    constructor(props?: PageProps) {
        super({
            ...props,
            withModules: true,
            mockWindowLocation: true,
            module: getRootModule(),
            RootContainer: () => (
                <>
                    <MessagesContainer />
                    <RootContainer />
                </>
            )
        });
    }

    async initialize() {
        await this.sagaTester.waitFor(initializedPage.type);
        this.wrapper.update();
    }

    async waitForRequests() {
        await this.sagaTester.waitFor(loadedRequests.type);
        this.wrapper.update();
    }

    async waitForCreatedRequest() {
        await this.sagaTester.waitFor(createdRequest.type);
        this.wrapper.update();
    }

    async waitForFailedToCreateRequest() {
        await this.sagaTester.waitFor(failedToCreateRequest.type);
        this.wrapper.update();
    }

    hasEmptyRequests() {
        return this.wrapper.find('.yb-empty-reconciliations-requests').length > 0;
    }

    async fillRequestForm(params: RequestParams) {
        await this.selectFirm(params.firmId);
        if (params.contractId) {
            await this.selectContract(params.contractId);
        }
        await this.selectPerson(params.personId);
        await this.choosePeriod(new Date(params.periodStart), new Date(params.periodEnd));
    }

    async submitRequestForm() {
        this.wrapper.find('button.yb-reconciliations-request__submit').simulate('submit');
    }

    async selectFirm(firmId: string) {
        this.fillReconciliationSelect(
            this.wrapper.find('.yb-reconciliations-request__firm'),
            firmId
        );
    }

    async selectContract(contractId: string) {
        this.fillReconciliationSelect(
            this.wrapper.find('.yb-reconciliations-request__contract'),
            contractId
        );
    }

    async selectPerson(personId: string) {
        this.fillReconciliationSelect(
            this.wrapper.find('.yb-reconciliations-request__person'),
            personId
        );
    }

    async choosePeriod(start: Date, end: Date) {
        (this.wrapper
            .find('.yb-reconciliations-request__range-picker')
            .at(0)
            .prop('onChange') as Function)({ value: { start, end } });
    }

    getMessage() {
        return this.wrapper.find('.yb-reconciliations-messages').text();
    }

    openRequestError() {
        this.wrapper.find('.yb-request-status').at(0).simulate('click');
        this.wrapper.update();
    }

    hideRequest() {
        this.wrapper.find('.yb-request-error__button_hide').at(0).simulate('click');
    }

    cloneRequest() {
        this.wrapper.find('.yb-request-error__button_clone').at(0).simulate('click');
    }

    async waitForProcessedRequest() {
        await this.sagaTester.waitFor(processedRequest.type);
        this.wrapper.update();
    }

    async waitForFailedToProcessRequest() {
        await this.sagaTester.waitFor(failedToProcessRequest.type);
        this.wrapper.update();
    }

    private async fillReconciliationSelect(select: ReactWrapper, value: string) {
        (select.find('.yb-reconciliation-select').at(0).prop('onChange') as Function)({
            id: value
        });
        this.wrapper.update();
    }
}
