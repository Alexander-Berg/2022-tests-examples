import React from 'react';
import { all } from 'redux-saga/effects';

import { Page, PageProps, MockData } from 'common/__tests__/common.page';
import { MessagesContainer } from 'common/containers/MessagesContainer';
import { messages } from 'common/reducers/messages';
import { watchAdminFetchError } from 'common/sagas/errors';

import invoicePageSaga from '../sagas';
import reducers from '../reducers';
import RootContainer from '../containers/RootContainer';
import {
    RECEIVE_INVOICE,
    RECEIVE_ACTS,
    RECEIVE_CONSUMES,
    RECEIVE_OPERATIONS,
    INVOICE
} from '../actions';
import { initializedSection } from '../actions/invoice-transfers/section';
import { unlockedItem } from '../actions/invoice-transfers';
import { failedToUnlockItem } from '../actions/invoice-transfers/list';
import {
    changeAmount,
    changeInvoiceExternalId,
    changeType,
    failedToSaveForm,
    savedForm
} from '../actions/invoice-transfers/form';
import { InvoiceTransferType } from '../types/invoice-transfers/states';

interface InvoicePageProps extends PageProps {
    invoicePageMocks: Record<string, MockData | undefined>;
}

const Root = () => (
    <>
        <RootContainer />
        <MessagesContainer />
    </>
);

function* rootSaga() {
    yield all([watchAdminFetchError(), invoicePageSaga()]);
}

export class InvoicePage extends Page {
    constructor(invoiceProps: InvoicePageProps) {
        let { invoicePageMocks, ...props } = invoiceProps;

        if (invoicePageMocks) {
            const {
                invoice,
                personforms,
                edoTypes,
                serviceCodeList,
                objectPermissions,
                urPersonforms,
                invoiceActs,
                invoiceConsumes,
                withdraw,
                invoiceOperations,
                exportState,
                oebsData
            } = invoicePageMocks;

            props.mocks = props.mocks ?? {};
            props.mocks.fetchGet = [
                invoice,
                personforms,
                edoTypes,
                serviceCodeList,
                objectPermissions,
                urPersonforms,
                invoiceActs,
                invoiceConsumes,
                withdraw,
                invoiceOperations,
                exportState,
                oebsData
            ].filter(Boolean) as MockData[];
        }

        super({
            ...props,
            rootSaga,
            reducers: { ...reducers, messages },
            RootContainer: Root,
            initialState: {
                isAdmin: true
            }
        });
    }

    async initializePage() {
        await this.sagaTester.waitFor(RECEIVE_INVOICE);
        await this.sagaTester.waitFor(RECEIVE_ACTS);
        await this.sagaTester.waitFor(RECEIVE_CONSUMES);
        await this.sagaTester.waitFor(RECEIVE_OPERATIONS);
        await this.sagaTester.waitFor(INVOICE.RECEIVE_OEBS_DATA);
        this.wrapper.update();
    }

    async initializeInvoiceTransfersSection() {
        await this.sagaTester.waitFor(initializedSection.type);
        this.wrapper.update();
    }

    hasInvoiceTransfersSection() {
        return this.wrapper.find('.yb-invoice-transfers').length > 0;
    }

    hasInvoiceTransfersList() {
        return this.wrapper.find('.yb-invoice-transfers-list').length > 0;
    }

    hasInvoiceTransfersForm() {
        return this.wrapper.find('.yb-invoice-transfers-form').length > 0;
    }

    async unlockInvoiceTransfer() {
        this.clickToUnlockInvoiceTransfer();
        await this.sagaTester.waitFor(unlockedItem.type);
        this.wrapper.update();
    }

    async unlockInvoiceTransferWithFailure() {
        this.clickToUnlockInvoiceTransfer();
        await this.sagaTester.waitFor(failedToUnlockItem.type);
        this.wrapper.update();
    }

    async enterDestinationInvoiceId(value: string) {
        (this.getInvoiceExternalIdInput().prop('onChange') as Function)({ target: { value } });
        await this.sagaTester.waitFor(changeInvoiceExternalId.type);
        this.wrapper.update();
    }

    async createInvoiceTransfer() {
        this.submitInvoiceTransferForm();
        await this.sagaTester.waitFor(savedForm.type);
        this.wrapper.update();
    }

    async changeInvoiceTransferType(type: InvoiceTransferType) {
        (this.wrapper
            .find(`.yb-invoice-transfers-form__option_${type} Radiobox`)
            .prop('onChange') as Function)();
        await this.sagaTester.waitFor(changeType.type);
        this.wrapper.update();
    }

    async enterInvoiceTransferAmount(value: string) {
        (this.getInvoiceTransferAmountInput().prop('onChange') as Function)({ target: { value } });
        await this.sagaTester.waitFor(changeAmount.type);
        this.wrapper.update();
    }

    async createInvoiceTransferWithFailure() {
        this.submitInvoiceTransferForm();
        await this.sagaTester.waitFor(failedToSaveForm.type);
        this.wrapper.update();
    }

    getAvailableTransferAmount() {
        return this.getInvoiceTransferAmountInput().prop('value');
    }

    getPageMessageText() {
        return this.wrapper.find('.yb-messages__text').text();
    }

    hasErrorInDestinationInvoiceId() {
        return this.getInvoiceExternalIdInput().prop('aria-invalid');
    }

    hasErrorInInvoiceTransferAmount() {
        return this.getInvoiceTransferAmountInput().prop('aria-invalid');
    }

    private clickToUnlockInvoiceTransfer() {
        this.wrapper.find('.yb-invoice-transfers-list__unlock button').simulate('click');
    }

    private submitInvoiceTransferForm() {
        this.wrapper.find('.yb-invoice-transfers-form').simulate('submit');
    }

    private getInvoiceExternalIdInput() {
        return this.wrapper.find('.yb-invoice-transfers-form__invoice-external-id input');
    }

    private getInvoiceTransferAmountInput() {
        return this.wrapper.find('.yb-invoice-transfers-form__amount input');
    }
}
