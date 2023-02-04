import Enzyme from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';
import { DeferpaysPage } from './page';

import { fullPerms, services, intercompanies } from './data';
import {
    payOffOne,
    confirmOne,
    deleteOne,
    confirmThree,
    deleteThree,
    confirmTwo,
    deleteTwo,
    blockWithoutFictive,
    blockWithFictive
} from './fictive.data';
import { ActionAction } from '../actions';
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

        test('проверяет выставление предварительного счета на счете с 1 заказом', async () => {
            expect.assertions(9);
            const {
                clientId,
                deferpayId,
                actionName,
                actionDate,
                client,
                deferpayContracts,
                deferpayList,
                action
            } = payOffOne;

            const page = new DeferpaysPage({
                perms: fullPerms,
                mocks: {
                    requestGet: [services, intercompanies],
                    fetchGet: [client, deferpayContracts, deferpayList],
                    fetchPost: [action]
                },
                windowLocationSearch: '?client_id=' + clientId
            });

            await page.initializePage();

            page.fillCheckbox(deferpayId);
            page.fillActionName(actionName);
            page.fillActionDate(actionDate);
            page.submitActionForm();
            await page.sagaTester.waitFor(ActionAction.RECEIVE); // Это тоже expect

            expect(page.request.get).toHaveBeenCalledTimes(2);
            expect(page.request.get).toHaveBeenNthCalledWith(1, services.request);
            expect(page.request.get).toHaveBeenNthCalledWith(2, intercompanies.request);

            expect(page.fetchGet).toHaveBeenCalledTimes(4); // Один лишний, тк он подгружает список и не интересен
            expect(page.fetchGet).toHaveBeenNthCalledWith(1, ...client.request);
            expect(page.fetchGet).toHaveBeenNthCalledWith(2, ...deferpayContracts.request);
            expect(page.fetchGet).toHaveBeenNthCalledWith(3, ...deferpayList.request);

            expect(page.fetchPost).toHaveBeenCalledTimes(1);
            expect(page.fetchPost).toHaveBeenNthCalledWith(1, ...action.request);
        });

        // выставление предварительного с 3 заказами проверяется в hermione

        // FIXME: https://st.yandex-team.ru/BALANCE-34763 починить исчезание выпадашки действия при выборе фиктивных счетов на разные заказы  и написать тест на базе test_fictive_2_invoices

        test('проверяет подтверждение предвательного счета с 1 заказом', async () => {
            expect.assertions(9);
            const {
                clientId,
                deferpayId,
                actionName,
                client,
                deferpayContracts,
                deferpayList,
                action
            } = confirmOne;

            const page = new DeferpaysPage({
                perms: fullPerms,
                mocks: {
                    requestGet: [services, intercompanies],
                    fetchGet: [client, deferpayContracts, deferpayList],
                    fetchPost: [action]
                },
                windowLocationSearch: '?client_id=' + clientId
            });

            await page.initializePage();

            page.fillCheckbox(deferpayId);
            page.fillActionName(actionName);
            page.submitActionForm();
            await page.sagaTester.waitFor(ActionAction.RECEIVE); // Это тоже expect

            expect(page.request.get).toHaveBeenCalledTimes(2);
            expect(page.request.get).toHaveBeenNthCalledWith(1, services.request);
            expect(page.request.get).toHaveBeenNthCalledWith(2, intercompanies.request);

            expect(page.fetchGet).toHaveBeenCalledTimes(4); // Один лишний, тк он подгружает список и не интересен
            expect(page.fetchGet).toHaveBeenNthCalledWith(1, ...client.request);
            expect(page.fetchGet).toHaveBeenNthCalledWith(2, ...deferpayContracts.request);
            expect(page.fetchGet).toHaveBeenNthCalledWith(3, ...deferpayList.request);

            expect(page.fetchPost).toHaveBeenCalledTimes(1);
            expect(page.fetchPost).toHaveBeenNthCalledWith(1, ...action.request);
        });

        test('проверяет удаление предвательного счета с 1 заказом', async () => {
            expect.assertions(9);
            const {
                clientId,
                deferpayId,
                actionName,
                client,
                deferpayContracts,
                deferpayList,
                action
            } = deleteOne;

            const page = new DeferpaysPage({
                perms: fullPerms,
                mocks: {
                    requestGet: [services, intercompanies],
                    fetchGet: [client, deferpayContracts, deferpayList],
                    fetchPost: [action]
                },
                windowLocationSearch: '?client_id=' + clientId
            });

            await page.initializePage();

            page.fillCheckbox(deferpayId);
            page.fillActionName(actionName);
            page.submitActionForm();
            await page.sagaTester.waitFor(ActionAction.RECEIVE); // Это тоже expect

            expect(page.request.get).toHaveBeenCalledTimes(2);
            expect(page.request.get).toHaveBeenNthCalledWith(1, services.request);
            expect(page.request.get).toHaveBeenNthCalledWith(2, intercompanies.request);

            expect(page.fetchGet).toHaveBeenCalledTimes(4); // Один лишний, тк он подгружает список и не интересен
            expect(page.fetchGet).toHaveBeenNthCalledWith(1, ...client.request);
            expect(page.fetchGet).toHaveBeenNthCalledWith(2, ...deferpayContracts.request);
            expect(page.fetchGet).toHaveBeenNthCalledWith(3, ...deferpayList.request);

            expect(page.fetchPost).toHaveBeenCalledTimes(1);
            expect(page.fetchPost).toHaveBeenNthCalledWith(1, ...action.request);
        });

        test('проверяет подтверждение предвательного счета с 3 заказами', async () => {
            expect.assertions(9);
            const {
                clientId,
                deferpayId,
                actionName,
                client,
                deferpayContracts,
                deferpayList,
                action
            } = confirmThree;

            const page = new DeferpaysPage({
                perms: fullPerms,
                mocks: {
                    requestGet: [services, intercompanies],
                    fetchGet: [client, deferpayContracts, deferpayList],
                    fetchPost: [action]
                },
                windowLocationSearch: '?client_id=' + clientId
            });

            await page.initializePage();

            page.fillCheckbox(deferpayId);
            page.fillActionName(actionName);
            page.submitActionForm();
            await page.sagaTester.waitFor(ActionAction.RECEIVE); // Это тоже expect

            expect(page.request.get).toHaveBeenCalledTimes(2);
            expect(page.request.get).toHaveBeenNthCalledWith(1, services.request);
            expect(page.request.get).toHaveBeenNthCalledWith(2, intercompanies.request);

            expect(page.fetchGet).toHaveBeenCalledTimes(4); // Один лишний, тк он подгружает список и не интересен
            expect(page.fetchGet).toHaveBeenNthCalledWith(1, ...client.request);
            expect(page.fetchGet).toHaveBeenNthCalledWith(2, ...deferpayContracts.request);
            expect(page.fetchGet).toHaveBeenNthCalledWith(3, ...deferpayList.request);

            expect(page.fetchPost).toHaveBeenCalledTimes(1);
            expect(page.fetchPost).toHaveBeenNthCalledWith(1, ...action.request);
        });

        test('проверяет удаление предвательного счета с 3 заказами', async () => {
            expect.assertions(9);
            const {
                clientId,
                deferpayId,
                actionName,
                client,
                deferpayContracts,
                deferpayList,
                action
            } = deleteThree;

            const page = new DeferpaysPage({
                perms: fullPerms,
                mocks: {
                    requestGet: [services, intercompanies],
                    fetchGet: [client, deferpayContracts, deferpayList],
                    fetchPost: [action]
                },
                windowLocationSearch: '?client_id=' + clientId
            });

            await page.initializePage();

            page.fillCheckbox(deferpayId);
            page.fillActionName(actionName);
            page.submitActionForm();
            await page.sagaTester.waitFor(ActionAction.RECEIVE); // Это тоже expect

            expect(page.request.get).toHaveBeenCalledTimes(2);
            expect(page.request.get).toHaveBeenNthCalledWith(1, services.request);
            expect(page.request.get).toHaveBeenNthCalledWith(2, intercompanies.request);

            expect(page.fetchGet).toHaveBeenCalledTimes(4); // Один лишний, тк он подгружает список и не интересен
            expect(page.fetchGet).toHaveBeenNthCalledWith(1, ...client.request);
            expect(page.fetchGet).toHaveBeenNthCalledWith(2, ...deferpayContracts.request);
            expect(page.fetchGet).toHaveBeenNthCalledWith(3, ...deferpayList.request);

            expect(page.fetchPost).toHaveBeenCalledTimes(1);
            expect(page.fetchPost).toHaveBeenNthCalledWith(1, ...action.request);
        });

        test('проверяет подтверждение нескольких предвательных счетов с 2 заказами', async () => {
            expect.assertions(9);
            const {
                clientId,
                deferpayIds,
                actionName,
                client,
                deferpayContracts,
                deferpayList,
                action
            } = confirmTwo;

            const page = new DeferpaysPage({
                perms: fullPerms,
                mocks: {
                    requestGet: [services, intercompanies],
                    fetchGet: [client, deferpayContracts, deferpayList],
                    fetchPost: [action]
                },
                windowLocationSearch: '?client_id=' + clientId
            });

            await page.initializePage();

            page.fillCheckbox(deferpayIds[0]);
            page.fillCheckbox(deferpayIds[1]);
            page.fillActionName(actionName);
            page.submitActionForm();
            await page.sagaTester.waitFor(ActionAction.RECEIVE); // Это тоже expect

            expect(page.request.get).toHaveBeenCalledTimes(2);
            expect(page.request.get).toHaveBeenNthCalledWith(1, services.request);
            expect(page.request.get).toHaveBeenNthCalledWith(2, intercompanies.request);

            expect(page.fetchGet).toHaveBeenCalledTimes(4); // Один лишний, тк он подгружает список и не интересен
            expect(page.fetchGet).toHaveBeenNthCalledWith(1, ...client.request);
            expect(page.fetchGet).toHaveBeenNthCalledWith(2, ...deferpayContracts.request);
            expect(page.fetchGet).toHaveBeenNthCalledWith(3, ...deferpayList.request);

            expect(page.fetchPost).toHaveBeenCalledTimes(1);
            expect(page.fetchPost).toHaveBeenNthCalledWith(1, ...action.request);
        });

        test('проверяет удаление нескольких предвательных счетов с 2 заказами', async () => {
            expect.assertions(9);
            const {
                clientId,
                deferpayIds,
                actionName,

                client,
                deferpayContracts,
                deferpayList,
                action
            } = deleteTwo;

            const page = new DeferpaysPage({
                perms: fullPerms,
                mocks: {
                    requestGet: [services, intercompanies],
                    fetchGet: [client, deferpayContracts, deferpayList],
                    fetchPost: [action]
                },
                windowLocationSearch: '?client_id=' + clientId
            });

            await page.initializePage();

            page.fillCheckbox(deferpayIds[0]);
            page.fillCheckbox(deferpayIds[1]);
            page.fillActionName(actionName);
            page.submitActionForm();
            await page.sagaTester.waitFor(ActionAction.RECEIVE); // Это тоже expect

            expect(page.request.get).toHaveBeenCalledTimes(2);
            expect(page.request.get).toHaveBeenNthCalledWith(1, services.request);
            expect(page.request.get).toHaveBeenNthCalledWith(2, intercompanies.request);

            expect(page.fetchGet).toHaveBeenCalledTimes(4); // Один лишний, тк он подгружает список и не интересен
            expect(page.fetchGet).toHaveBeenNthCalledWith(1, ...client.request);
            expect(page.fetchGet).toHaveBeenNthCalledWith(2, ...deferpayContracts.request);
            expect(page.fetchGet).toHaveBeenNthCalledWith(3, ...deferpayList.request);

            expect(page.fetchPost).toHaveBeenCalledTimes(1);
            expect(page.fetchPost).toHaveBeenNthCalledWith(1, ...action.request);
        });

        test('проверяет блокирование выбора счета с предварительным при выборе без предварительного', async () => {
            expect.assertions(8);
            const {
                clientId,
                deferpayId,
                disabledDeferpayId,
                client,
                deferpayContracts,
                deferpayList
            } = blockWithoutFictive;

            const page = new DeferpaysPage({
                perms: fullPerms,
                mocks: {
                    requestGet: [services, intercompanies],
                    fetchGet: [client, deferpayContracts, deferpayList]
                },
                windowLocationSearch: '?client_id=' + clientId
            });

            await page.initializePage();

            page.fillCheckbox(deferpayId);

            expect(page.getCheckbox(disabledDeferpayId).prop('disabled')).toBe(true);

            expect(page.request.get).toHaveBeenCalledTimes(2);
            expect(page.request.get).toHaveBeenNthCalledWith(1, services.request);
            expect(page.request.get).toHaveBeenNthCalledWith(2, intercompanies.request);

            expect(page.fetchGet).toHaveBeenCalledTimes(3);
            expect(page.fetchGet).toHaveBeenNthCalledWith(1, ...client.request);
            expect(page.fetchGet).toHaveBeenNthCalledWith(2, ...deferpayContracts.request);
            expect(page.fetchGet).toHaveBeenNthCalledWith(3, ...deferpayList.request);
        });

        test('проверяет блокирование выбора счета без предварительного при выборе с предварительным', async () => {
            expect.assertions(8);
            const {
                clientId,
                deferpayId,
                disabledDeferpayId,
                client,
                deferpayContracts,
                deferpayList
            } = blockWithFictive;

            const page = new DeferpaysPage({
                perms: fullPerms,
                mocks: {
                    requestGet: [services, intercompanies],
                    fetchGet: [client, deferpayContracts, deferpayList]
                },
                windowLocationSearch: '?client_id=' + clientId
            });

            await page.initializePage();

            page.fillCheckbox(deferpayId);

            expect(page.getCheckbox(disabledDeferpayId).prop('disabled')).toBe(true);

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
