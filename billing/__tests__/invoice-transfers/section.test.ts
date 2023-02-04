import Enzyme from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';
// @ts-ignore
import urijs from 'urijs';

import 'common/utils/numeral';
import { Permissions } from 'common/constants';
import { initializeDesktopRegistry } from 'common/__tests__/registry';

import { InvoicePage } from '../page';
import { perms, mocks } from '../page.data';
import { mocks as invoiceTransfersMocks } from './data';
import { InvoiceTransferType } from '../../types/invoice-transfers/states';

jest.mock('common/utils/old-fetch');
jest.mock('common/utils/request');
jest.mock('urijs', () => () => ({ search: () => ({ invoice_id: '123' }) }));

Enzyme.configure({ adapter: new Adapter() });

describe('admin - invoice', () => {
    beforeAll(initializeDesktopRegistry);

    describe('invoice-transfers', () => {
        afterEach(() => {
            jest.clearAllMocks();
        });

        test('проверка отсутствия блока из-за прав', async () => {
            expect.assertions(1);

            const page = new InvoicePage({
                perms,
                invoicePageMocks: mocks,
                mocks: {
                    requestGet: [invoiceTransfersMocks.invoiceTransfers]
                }
            });

            await page.initializePage();

            expect(page.hasInvoiceTransfersSection()).toBeFalsy();
        });

        test('проверка отсутствия списка переводов из-за права', async () => {
            expect.assertions(2);

            const page = new InvoicePage({
                perms: perms.concat([Permissions.DO_INVOICE_TRANSFER]),
                invoicePageMocks: mocks,
                mocks: {
                    requestGet: [invoiceTransfersMocks.invoiceTransfers]
                }
            });

            await page.initializePage();
            await page.initializeInvoiceTransfersSection();

            expect(page.hasInvoiceTransfersList()).toBeFalsy();
            expect(page.hasInvoiceTransfersForm()).toBeTruthy();
        });

        test('проверка отсутствия списка переводов из-за отсутствия переводов', async () => {
            expect.assertions(2);

            const page = new InvoicePage({
                perms: perms.concat([Permissions.DO_INVOICE_TRANSFER, Permissions.VIEW_INVOICES]),
                invoicePageMocks: mocks,
                mocks: {
                    requestGet: [invoiceTransfersMocks.emptyInvoiceTransfers]
                }
            });

            await page.initializePage();
            await page.initializeInvoiceTransfersSection();

            expect(page.hasInvoiceTransfersList()).toBeFalsy();
            expect(page.hasInvoiceTransfersForm()).toBeTruthy();
        });

        test('проверка отсутствия формы перевода из-за отсутствия права', async () => {
            expect.assertions(2);

            const page = new InvoicePage({
                perms: perms.concat([Permissions.VIEW_INVOICES]),
                invoicePageMocks: mocks,
                mocks: {
                    requestGet: [invoiceTransfersMocks.invoiceTransfers]
                }
            });

            await page.initializePage();
            await page.initializeInvoiceTransfersSection();

            expect(page.hasInvoiceTransfersList()).toBeTruthy();
            expect(page.hasInvoiceTransfersForm()).toBeFalsy();
        });

        test('проверка отсутствия формы перевода из-за недостаточных средств', async () => {
            expect.assertions(2);

            const page = new InvoicePage({
                perms: perms.concat([Permissions.DO_INVOICE_TRANSFER, Permissions.VIEW_INVOICES]),
                invoicePageMocks: {
                    ...mocks,
                    invoice: invoiceTransfersMocks.invoiceWithoutMoney
                },
                mocks: {
                    requestGet: [invoiceTransfersMocks.invoiceTransfers]
                }
            });

            await page.initializePage();
            await page.initializeInvoiceTransfersSection();

            expect(page.hasInvoiceTransfersList()).toBeTruthy();
            expect(page.hasInvoiceTransfersForm()).toBeFalsy();
        });

        test('проверка присутствия списка переводов и формы перевода', async () => {
            expect.assertions(3);

            const page = new InvoicePage({
                perms: perms.concat([Permissions.DO_INVOICE_TRANSFER, Permissions.VIEW_INVOICES]),
                invoicePageMocks: mocks,
                mocks: {
                    requestGet: [invoiceTransfersMocks.invoiceTransfers]
                }
            });

            await page.initializePage();
            await page.initializeInvoiceTransfersSection();

            expect(page.request.get).nthCalledWith(
                1,
                invoiceTransfersMocks.invoiceTransfers.request
            );
            expect(page.hasInvoiceTransfersList()).toBeTruthy();
            expect(page.hasInvoiceTransfersForm()).toBeTruthy();
        });

        test('успешная разблокировка перевода', async () => {
            expect.assertions(3);

            const page = new InvoicePage({
                perms: perms.concat([Permissions.DO_INVOICE_TRANSFER, Permissions.VIEW_INVOICES]),
                invoicePageMocks: mocks,
                mocks: {
                    requestGet: [
                        invoiceTransfersMocks.invoiceTransfers,
                        invoiceTransfersMocks.invoiceTransfers
                    ],
                    requestPost: [invoiceTransfersMocks.unlockInvoiceTransfer]
                }
            });

            await page.initializePage();
            await page.initializeInvoiceTransfersSection();

            expect(page.getAvailableTransferAmount()).toBe('850000.00');

            await page.unlockInvoiceTransfer();

            expect(page.request.post).nthCalledWith(
                1,
                invoiceTransfersMocks.unlockInvoiceTransfer.request
            );
            expect(page.getAvailableTransferAmount()).toBe('9999999');
        });

        test('неуспешная разблокировка перевода', async () => {
            expect.assertions(2);

            const page = new InvoicePage({
                perms: perms.concat([Permissions.DO_INVOICE_TRANSFER, Permissions.VIEW_INVOICES]),
                invoicePageMocks: mocks,
                mocks: {
                    requestGet: [invoiceTransfersMocks.invoiceTransfers],
                    requestPost: [invoiceTransfersMocks.failedToUnlockInvoiceTransfer]
                }
            });

            await page.initializePage();
            await page.initializeInvoiceTransfersSection();

            await page.unlockInvoiceTransferWithFailure();

            expect(page.request.post).nthCalledWith(
                1,
                invoiceTransfersMocks.failedToUnlockInvoiceTransfer.request
            );
            expect(page.getPageMessageText()).toBe('INTERNAL_UNKNOWN_ERROR_intl');
        });

        test('валидация при создании перевода (пустой счёт назначения)', async () => {
            expect.assertions(1);

            const page = new InvoicePage({
                perms: perms.concat([Permissions.DO_INVOICE_TRANSFER]),
                invoicePageMocks: mocks,
                mocks: {
                    requestGet: [invoiceTransfersMocks.invoiceTransfers]
                }
            });

            await page.initializePage();
            await page.initializeInvoiceTransfersSection();

            await page.enterDestinationInvoiceId('   ');

            expect(page.hasErrorInDestinationInvoiceId()).toBeTruthy();
        });

        test('валидация при создании перевода (в сумме перевода не число)', async () => {
            expect.assertions(1);

            const page = new InvoicePage({
                perms: perms.concat([Permissions.DO_INVOICE_TRANSFER]),
                invoicePageMocks: mocks,
                mocks: {
                    requestGet: [invoiceTransfersMocks.invoiceTransfers]
                }
            });

            await page.initializePage();
            await page.initializeInvoiceTransfersSection();

            await page.changeInvoiceTransferType(InvoiceTransferType.SOME_MONEY);
            await page.enterInvoiceTransferAmount('asd');

            expect(page.hasErrorInInvoiceTransferAmount()).toBeTruthy();
        });

        test('валидация при создании перевода (сумма перевода превышает доступную сумму)', async () => {
            expect.assertions(1);

            const page = new InvoicePage({
                perms: perms.concat([Permissions.DO_INVOICE_TRANSFER]),
                invoicePageMocks: mocks,
                mocks: {
                    requestGet: [invoiceTransfersMocks.invoiceTransfers]
                }
            });

            await page.initializePage();
            await page.initializeInvoiceTransfersSection();

            await page.changeInvoiceTransferType(InvoiceTransferType.SOME_MONEY);
            await page.enterInvoiceTransferAmount('850001.00');

            expect(page.hasErrorInInvoiceTransferAmount()).toBeTruthy();
        });

        test('возврат максимального значения при переключении на перевод со всеми средствами', async () => {
            expect.assertions(2);

            const page = new InvoicePage({
                perms: perms.concat([Permissions.DO_INVOICE_TRANSFER]),
                invoicePageMocks: mocks,
                mocks: {
                    requestGet: [invoiceTransfersMocks.invoiceTransfers]
                }
            });

            await page.initializePage();
            await page.initializeInvoiceTransfersSection();

            expect(page.getAvailableTransferAmount()).toBe('850000.00');

            await page.changeInvoiceTransferType(InvoiceTransferType.SOME_MONEY);
            await page.enterInvoiceTransferAmount('850001.00');
            await page.changeInvoiceTransferType(InvoiceTransferType.ALL_MONEY);

            expect(page.getAvailableTransferAmount()).toBe('850000.00');
        });

        test('успешное создание перевода (со всеми средствами)', async () => {
            expect.assertions(3);

            const page = new InvoicePage({
                perms: perms.concat([Permissions.DO_INVOICE_TRANSFER, Permissions.VIEW_INVOICES]),
                invoicePageMocks: mocks,
                mocks: {
                    requestGet: [
                        invoiceTransfersMocks.invoiceTransfers,
                        invoiceTransfersMocks.invoiceTransfers
                    ],
                    requestPost: [invoiceTransfersMocks.createInvoiceTransferWithAllMoney]
                }
            });

            await page.initializePage();
            await page.initializeInvoiceTransfersSection();

            await page.enterDestinationInvoiceId('234');

            await page.createInvoiceTransfer();

            expect(page.request.post).nthCalledWith(
                1,
                invoiceTransfersMocks.createInvoiceTransferWithAllMoney.request
            );
            expect(page.getPageMessageText()).toBe(
                'ID_Invoice_invoice-transfers_send-money_success'
            );
            expect(page.hasInvoiceTransfersForm()).toBeFalsy();
        });

        test('успешное создание перевода (с введённой суммой)', async () => {
            expect.assertions(3);

            const page = new InvoicePage({
                perms: perms.concat([Permissions.DO_INVOICE_TRANSFER, Permissions.VIEW_INVOICES]),
                invoicePageMocks: mocks,
                mocks: {
                    requestGet: [
                        invoiceTransfersMocks.invoiceTransfers,
                        invoiceTransfersMocks.invoiceTransfers
                    ],
                    requestPost: [invoiceTransfersMocks.createInvoiceTransferWithSomeMoney]
                }
            });

            await page.initializePage();
            await page.initializeInvoiceTransfersSection();

            await page.enterDestinationInvoiceId('234');
            await page.changeInvoiceTransferType(InvoiceTransferType.SOME_MONEY);
            await page.enterInvoiceTransferAmount('777');

            await page.createInvoiceTransfer();

            expect(page.request.post).nthCalledWith(
                1,
                invoiceTransfersMocks.createInvoiceTransferWithSomeMoney.request
            );
            expect(page.getPageMessageText()).toBe(
                'ID_Invoice_invoice-transfers_send-money_success'
            );
            expect(page.getAvailableTransferAmount()).toBe('77777');
        });

        test('неуспешное создание перевода', async () => {
            expect.assertions(1);

            const page = new InvoicePage({
                perms: perms.concat([Permissions.DO_INVOICE_TRANSFER, Permissions.VIEW_INVOICES]),
                invoicePageMocks: mocks,
                mocks: {
                    requestGet: [invoiceTransfersMocks.invoiceTransfers],
                    requestPost: [invoiceTransfersMocks.failedTocreateInvoiceTransfer]
                }
            });

            await page.initializePage();
            await page.initializeInvoiceTransfersSection();

            await page.enterDestinationInvoiceId('234');

            await page.createInvoiceTransferWithFailure();

            expect(page.getPageMessageText()).toBe('INTERNAL_UNKNOWN_ERROR_intl');
        });
    });
});
