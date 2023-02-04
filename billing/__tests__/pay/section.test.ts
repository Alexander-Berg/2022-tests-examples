import Enzyme from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';
// @ts-ignore
import urijs from 'urijs';

import 'common/utils/numeral';
import { initializeDesktopRegistry } from 'common/__tests__/registry';

import { InvoicePage } from '../page';
import { perms, mocks } from '../page.data';
import { permsReadonly, mocks as payMocks } from './data';

jest.mock('common/utils/old-fetch');
jest.mock('common/utils/request');
jest.mock('urijs', () => () => ({ search: () => ({ invoice_id: '123' }) }));

Enzyme.configure({ adapter: new Adapter() });

describe('admin - invoice', () => {
    beforeAll(initializeDesktopRegistry);

    describe('pay', () => {
        afterEach(() => {
            jest.clearAllMocks();
        });

        test('проверка красной подсветки, предоплатный счет', async () => {
            expect.assertions(14);

            const page = new InvoicePage({
                perms,
                invoicePageMocks: mocks
            });

            await page.initializePage();

            // admin/pages/invoice/components/ConfirmPayment/style.module.scss:7
            expect(
                page.wrapper.find('section.yb-invoice-confirm-payment').prop('className')
            ).toMatch(/\bsuspect1\b/);

            expect(page.fetchGet).toHaveBeenCalledTimes(12);
            expect(page.fetchGet).toHaveBeenNthCalledWith(1, ...mocks.invoice.request);
            expect(page.fetchGet).toHaveBeenNthCalledWith(2, ...mocks.personforms.request);
            expect(page.fetchGet).toHaveBeenNthCalledWith(3, ...mocks.edoTypes.request);
            expect(page.fetchGet).toHaveBeenNthCalledWith(4, ...mocks.serviceCodeList.request);
            expect(page.fetchGet).toHaveBeenNthCalledWith(5, ...mocks.objectPermissions.request);
            expect(page.fetchGet).toHaveBeenNthCalledWith(6, ...mocks.urPersonforms.request);
            expect(page.fetchGet).toHaveBeenNthCalledWith(7, ...mocks.invoiceActs.request);
            expect(page.fetchGet).toHaveBeenNthCalledWith(8, ...mocks.invoiceConsumes.request);
            expect(page.fetchGet).toHaveBeenNthCalledWith(9, ...mocks.withdraw.request);
            expect(page.fetchGet).toHaveBeenNthCalledWith(10, ...mocks.invoiceOperations.request);
            expect(page.fetchGet).toHaveBeenNthCalledWith(11, ...mocks.exportState.request);
            expect(page.fetchGet).toHaveBeenNthCalledWith(12, ...mocks.oebsData.request);
        });

        test('нет блока оплаты без права CreateBankPayments', async () => {
            expect.assertions(3);

            const page = new InvoicePage({
                perms: permsReadonly,
                invoicePageMocks: {
                    ...mocks,
                    withdraw: undefined,
                    objectPermissions: payMocks.readonlyObjectPermissions
                }
            });

            await page.initializePage();

            expect(page.wrapper.exists('.yb-invoice-confirm-payment')).toBe(false);

            expect(page.fetchGet).toHaveBeenCalledTimes(11);
            expect(page.fetchGet).toHaveBeenNthCalledWith(
                5,
                ...payMocks.readonlyObjectPermissions.request
            );
        });
    });
});
