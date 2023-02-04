import Enzyme from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';

import { initializeMobileRegistry, initializeDesktopRegistry } from 'common/__tests__/registry';
import { registry as commonRegistry } from 'common/components/registry';

import { ReconciliationsDesktopPage } from './page.desktop';
import { ReconciliationsMobilePage } from './page.mobile';
import { mocks } from './page.mocks';
import { Messages } from '../components/Messages';

jest.mock('common/utils/old-fetch');
jest.mock('common/utils/request');
jest.mock('common/utils/intl');

Enzyme.configure({ adapter: new Adapter() });

describe('страница актов сверки', () => {
    describeReconciliationsPageTests(
        'десктопная версия',
        ReconciliationsDesktopPage,
        initializeDesktopRegistry
    );
    describeReconciliationsPageTests(
        'мобильная версия',
        ReconciliationsMobilePage,
        initializeMobileRegistry
    );
});

function describeReconciliationsPageTests(
    title: string,
    ReconciliationsPage: typeof ReconciliationsDesktopPage | typeof ReconciliationsMobilePage,
    initializeRegistry: Function
) {
    return describe(title, () => {
        afterEach(jest.resetAllMocks);

        beforeAll(() => {
            initializeRegistry();
            commonRegistry.set('Messages', Messages);
        });

        it('пустой список запросов', async () => {
            expect.assertions(6);

            const page = new ReconciliationsPage({
                mocks: {
                    requestGet: [
                        mocks.client,
                        mocks.availability,
                        mocks.personsFirms,
                        mocks.lastClosedPeriods,
                        mocks.emptyRequests
                    ]
                }
            });

            await page.initialize();
            await page.waitForRequests();

            expect(page.request.get).nthCalledWith(1, mocks.client.request);
            expect(page.request.get).nthCalledWith(2, mocks.availability.request);
            expect(page.request.get).nthCalledWith(3, mocks.personsFirms.request);
            expect(page.request.get).nthCalledWith(4, mocks.lastClosedPeriods.request);
            expect(page.request.get).nthCalledWith(5, mocks.emptyRequests.request);
            expect(page.hasEmptyRequests()).toBeTruthy();
        });

        it('непустой список запросов', async () => {
            expect.assertions(2);

            const page = new ReconciliationsPage({
                mocks: {
                    requestGet: [
                        mocks.client,
                        mocks.availability,
                        mocks.personsFirms,
                        mocks.lastClosedPeriods,
                        mocks.requests
                    ]
                }
            });

            await page.initialize();
            await page.waitForRequests();

            expect(page.request.get).nthCalledWith(5, mocks.requests.request);
            expect(page.getRequests().length).toBe(2);
        });

        it('непустой список запросов (ID клиента из урла)', async () => {
            expect.assertions(6);

            const page = new ReconciliationsPage({
                windowLocationSearch: '?clientId=234',
                mocks: {
                    requestGet: [
                        mocks.anotherClient,
                        mocks.availabilityForAnotherClient,
                        mocks.personsFirmsForAnotherClient,
                        mocks.lastClosedPeriodsForAnotherClient,
                        mocks.requestsForAnotherClient
                    ]
                }
            });

            await page.initialize();
            await page.waitForRequests();

            expect(page.request.get).nthCalledWith(1, mocks.anotherClient.request);
            expect(page.request.get).nthCalledWith(2, mocks.availabilityForAnotherClient.request);
            expect(page.request.get).nthCalledWith(3, mocks.personsFirmsForAnotherClient.request);
            expect(page.request.get).nthCalledWith(
                4,
                mocks.lastClosedPeriodsForAnotherClient.request
            );
            expect(page.request.get).nthCalledWith(5, mocks.requestsForAnotherClient.request);
            expect(page.getRequests().length).toBe(2);
        });

        it('успешное создание запроса', async () => {
            expect.assertions(3);

            const page = new ReconciliationsPage({
                mocks: {
                    requestGet: [
                        mocks.client,
                        mocks.availability,
                        mocks.personsFirms,
                        mocks.lastClosedPeriods,
                        mocks.requests,
                        mocks.requests
                    ],
                    requestPost: [mocks.createRequest]
                }
            });

            await page.initialize();
            await page.waitForRequests();

            await page.openRequestForm();
            await page.fillRequestForm({
                firmId: '123',
                contractId: '1839660/1',
                personId: '345',
                periodStart: '2022-05-14',
                periodEnd: '2022-05-15'
            });
            await page.submitRequestForm();

            await page.waitForCreatedRequest();

            expect(page.request.post).nthCalledWith(1, mocks.createRequest.request);
            expect(page.getMessage()).toBe('Готовим акт сверки. Отправим на asd@asd.asd');
            expect(page.getRequests().length).toBe(2);
        });

        it('успешное создание запроса (без фирмы)', async () => {
            expect.assertions(3);

            const page = new ReconciliationsPage({
                mocks: {
                    requestGet: [
                        mocks.client,
                        mocks.availability,
                        mocks.personsFirms,
                        mocks.lastClosedPeriods,
                        mocks.requests,
                        mocks.requests
                    ],
                    requestPost: [mocks.createRequestWithoutContract]
                }
            });

            await page.initialize();
            await page.waitForRequests();

            await page.openRequestForm();
            await page.fillRequestForm({
                firmId: '234',
                personId: '345',
                periodStart: '2022-05-14',
                periodEnd: '2022-05-15'
            });
            await page.submitRequestForm();

            await page.waitForCreatedRequest();

            expect(page.request.post).nthCalledWith(1, mocks.createRequestWithoutContract.request);
            expect(page.getMessage()).toBe('Готовим акт сверки. Отправим на asd@asd.asd');
            expect(page.getRequests().length).toBe(2);
        });

        it('создание запроса с ошибкой', async () => {
            expect.assertions(2);

            const page = new ReconciliationsPage({
                mocks: {
                    requestGet: [
                        mocks.client,
                        mocks.availability,
                        mocks.personsFirms,
                        mocks.lastClosedPeriods,
                        mocks.requests,
                        mocks.requests
                    ],
                    requestPost: [mocks.createRequestWithError]
                }
            });

            await page.initialize();
            await page.waitForRequests();

            await page.openRequestForm();
            await page.fillRequestForm({
                firmId: '123',
                contractId: '1839660/1',
                personId: '345',
                periodStart: '2022-05-14',
                periodEnd: '2022-05-15'
            });
            await page.submitRequestForm();

            await page.waitForFailedToCreateRequest();

            expect(page.request.post).nthCalledWith(1, mocks.createRequestWithError.request);
            expect(page.getMessage()).toBe('Неизвестная внутренняя ошибка.');
        });

        it('успешное скрытие запроса', async () => {
            expect.assertions(1);

            const page = new ReconciliationsPage({
                mocks: {
                    requestGet: [
                        mocks.client,
                        mocks.availability,
                        mocks.personsFirms,
                        mocks.lastClosedPeriods,
                        mocks.requests,
                        mocks.requests
                    ],
                    requestPost: [mocks.hideRequest]
                }
            });

            await page.initialize();
            await page.waitForRequests();

            await page.openRequestError();
            await page.hideRequest();

            await page.waitForProcessedRequest();

            expect(page.request.post).nthCalledWith(1, mocks.hideRequest.request);
        });

        it('скрытие запроса c ошибкой', async () => {
            expect.assertions(2);

            const page = new ReconciliationsPage({
                mocks: {
                    requestGet: [
                        mocks.client,
                        mocks.availability,
                        mocks.personsFirms,
                        mocks.lastClosedPeriods,
                        mocks.requests,
                        mocks.requests
                    ],
                    requestPost: [mocks.hideRequestWithError]
                }
            });

            await page.initialize();
            await page.waitForRequests();

            await page.openRequestError();
            await page.hideRequest();

            await page.waitForFailedToProcessRequest();

            expect(page.request.post).nthCalledWith(1, mocks.hideRequestWithError.request);
            expect(page.getMessage()).toBe('Неизвестная внутренняя ошибка.');
        });

        it('успешное клонирование запроса', async () => {
            expect.assertions(2);

            const page = new ReconciliationsPage({
                mocks: {
                    requestGet: [
                        mocks.client,
                        mocks.availability,
                        mocks.personsFirms,
                        mocks.lastClosedPeriods,
                        mocks.requests,
                        mocks.requests
                    ],
                    requestPost: [mocks.cloneRequest]
                }
            });

            await page.initialize();
            await page.waitForRequests();

            await page.openRequestError();
            await page.cloneRequest();

            await page.waitForProcessedRequest();

            expect(page.request.post).nthCalledWith(1, mocks.cloneRequest.request);
            expect(page.getMessage()).toBe('Готовим акт сверки. Отправим на asd@asd.asd');
        });

        it('клонирование запроса c ошибкой', async () => {
            expect.assertions(2);

            const page = new ReconciliationsPage({
                mocks: {
                    requestGet: [
                        mocks.client,
                        mocks.availability,
                        mocks.personsFirms,
                        mocks.lastClosedPeriods,
                        mocks.requests,
                        mocks.requests
                    ],
                    requestPost: [mocks.cloneRequestWithError]
                }
            });

            await page.initialize();
            await page.waitForRequests();

            await page.openRequestError();
            await page.cloneRequest();

            await page.waitForFailedToProcessRequest();

            expect(page.request.post).nthCalledWith(1, mocks.cloneRequestWithError.request);
            expect(page.getMessage()).toBe('Неизвестная внутренняя ошибка.');
        });
    });
}
