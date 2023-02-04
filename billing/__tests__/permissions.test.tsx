import Enzyme from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';
import { DeferpaysPage } from './page';

import { services, intercompanies } from './data';
import { noChangeRepaymentsStatus, noIssueInvoices, noViewPersons } from './permissions.data';
import { initializeDesktopRegistry } from 'common/__tests__/registry';

jest.mock('common/utils/old-fetch');
jest.mock('common/utils/request');

Enzyme.configure({ adapter: new Adapter() });

describe('admin', () => {
    beforeAll(initializeDesktopRegistry);

    describe('deferpays', () => {
        afterEach(() => {
            jest.resetAllMocks();
        });

        test('проверяет, что без права ChangeRepaymentsStatus нельзя подтвердить или удалить предварительный счет', async () => {
            expect.assertions(8);
            const {
                perms,
                client,
                deferpayContracts,
                deferpayList,
                clientId,
                deferpayId
            } = noChangeRepaymentsStatus;

            const page = new DeferpaysPage({
                perms,
                mocks: {
                    requestGet: [services, intercompanies],
                    fetchGet: [client, deferpayContracts, deferpayList]
                },
                windowLocationSearch: '?client_id=' + clientId
            });

            await page.initializePage();

            expect(page.getCheckbox(deferpayId).prop('disabled')).toBe(true);

            expect(page.request.get).toHaveBeenCalledTimes(2);
            expect(page.request.get).toHaveBeenNthCalledWith(1, services.request);
            expect(page.request.get).toHaveBeenNthCalledWith(2, intercompanies.request);

            expect(page.fetchGet).toHaveBeenCalledTimes(3);
            expect(page.fetchGet).toHaveBeenNthCalledWith(1, ...client.request);
            expect(page.fetchGet).toHaveBeenNthCalledWith(2, ...deferpayContracts.request);
            expect(page.fetchGet).toHaveBeenNthCalledWith(3, ...deferpayList.request);
        });

        test('проверяет, что без права IssueInvoices нельзя выставить предварительный счет', async () => {
            expect.assertions(8);
            const {
                perms,
                client,
                deferpayContracts,
                deferpayList,
                clientId,
                deferpayId
            } = noIssueInvoices;

            const page = new DeferpaysPage({
                perms,
                mocks: {
                    requestGet: [services, intercompanies],
                    fetchGet: [client, deferpayContracts, deferpayList]
                },
                windowLocationSearch: '?client_id=' + clientId
            });

            await page.initializePage();

            expect(page.getCheckbox(deferpayId).prop('disabled')).toBe(true);

            expect(page.request.get).toHaveBeenCalledTimes(2);
            expect(page.request.get).toHaveBeenNthCalledWith(1, services.request);
            expect(page.request.get).toHaveBeenNthCalledWith(2, intercompanies.request);

            expect(page.fetchGet).toHaveBeenCalledTimes(3);
            expect(page.fetchGet).toHaveBeenNthCalledWith(1, ...client.request);
            expect(page.fetchGet).toHaveBeenNthCalledWith(2, ...deferpayContracts.request);
            expect(page.fetchGet).toHaveBeenNthCalledWith(3, ...deferpayList.request);
        });

        test('проверяет, что без права ViewPersons нельзя выставить предварительный счет', async () => {
            expect.assertions(8);
            const { perms, client, deferpayContracts, deferpayList, clientId } = noViewPersons;

            const page = new DeferpaysPage({
                perms,
                mocks: {
                    requestGet: [services, intercompanies],
                    fetchGet: [client, deferpayContracts, deferpayList]
                },
                windowLocationSearch: '?client_id=' + clientId
            });

            await page.initializePage();

            expect(page.getPersonLink()).toHaveLength(0);

            expect(page.request.get).toHaveBeenCalledTimes(2);
            expect(page.request.get).toHaveBeenNthCalledWith(1, services.request);
            expect(page.request.get).toHaveBeenNthCalledWith(2, intercompanies.request);

            expect(page.fetchGet).toHaveBeenCalledTimes(3);
            expect(page.fetchGet).toHaveBeenNthCalledWith(1, ...client.request);
            expect(page.fetchGet).toHaveBeenNthCalledWith(2, ...deferpayContracts.request);
            expect(page.fetchGet).toHaveBeenNthCalledWith(3, ...deferpayList.request);
        });
    });
});
