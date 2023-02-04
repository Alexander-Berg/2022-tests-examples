jest.mock('auto-core/lib/marketing', () => ({
    getDataFromReact: () => (paymentModalResult),
    sendEvents: jest.fn(),
}));

const mockStore = require('autoru-frontend/mocks/mockStore').default;
const sendMarketingEventByReport = require('./sendMarketingEventByReport');
const marketingMock = require('auto-core/lib/marketing');
const contextMock = require('autoru-frontend/mocks/contextMock').default;

const paymentModalResult = {
    cost: 8800,
    ticket_id: 123456,
};

describe('карточка оффера', () => {
    const store = mockStore({
        config: {
            data: { pageType: 'card' },
        },
    });

    it('должен отправить цели при покупке одного отчёта', () => {
        const purchaseCount = 1;

        return store.dispatch(sendMarketingEventByReport(purchaseCount, paymentModalResult)).then(() => {
            expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith('CARS_OPEN_REPORT_CARD');
            expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith('PURCHASE_SINGLE_REPORT_CARD');
            expect(marketingMock.sendEvents).toHaveBeenCalledWith(paymentModalResult,
                [
                    { counter: 'adwords', type: 'buyReportInCard' },
                    { counter: 'adwords', type: 'conversionBuyReportDV360' },
                    { counter: 'adwords', type: 'conversionBuyReportCM360' },
                    { counter: 'adwords', type: 'conversionBuyReportCM360TAG' },
                ],
            );
        });
    });

    it('должен отправить цели при покупке пакета из 5 отчётов', () => {
        const purchaseCount = 5;

        return store.dispatch(sendMarketingEventByReport(purchaseCount, paymentModalResult)).then(() => {
            expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith('CARS_OPEN_REPORT_CARD');
            expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith('PURCHASE_BUNDLE-5_REPORT_CARD');
            expect(marketingMock.sendEvents).toHaveBeenCalledWith(paymentModalResult,
                [
                    { counter: 'adwords', type: 'buyReportPackageInCard' },
                    { counter: 'adwords', type: 'conversionBuyReportDV360' },
                    { counter: 'adwords', type: 'conversionBuyReportCM360' },
                    { counter: 'adwords', type: 'conversionBuyReportCM360TAG' },
                ],
            );
        });
    });

    it('должен отправить цели при покупке пакета отчётов', () => {
        const purchaseCount = 10;

        return store.dispatch(sendMarketingEventByReport(purchaseCount, paymentModalResult)).then(() => {
            expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith('CARS_OPEN_REPORT_CARD');
            expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith('PURCHASE_BUNDLE_REPORT_CARD');
            expect(marketingMock.sendEvents).toHaveBeenCalledWith(paymentModalResult,
                [
                    { counter: 'adwords', type: 'buyReportPackageInCard' },
                    { counter: 'adwords', type: 'conversionBuyReportDV360' },
                    { counter: 'adwords', type: 'conversionBuyReportCM360' },
                    { counter: 'adwords', type: 'conversionBuyReportCM360TAG' },
                ],
            );
        });
    });

    it('если для просмотра отчёта расходуется пакет, не должен отправлять метрики о покупке', () => {
        const purchaseCount = null;

        return store.dispatch(sendMarketingEventByReport(purchaseCount, paymentModalResult)).then(() => {
            expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith('CARS_OPEN_REPORT_CARD');
            expect(contextMock.metrika.reachGoal).not.toHaveBeenCalledWith('PURCHASE_SINGLE_REPORT_CARD');
            expect(contextMock.metrika.reachGoal).not.toHaveBeenCalledWith('PURCHASE_BUNDLE_REPORT_CARD');
            expect(marketingMock.sendEvents).not.toHaveBeenCalled();
        });
    });
});

describe('страница отчёта', () => {
    const store = mockStore({
        config: {
            data: { pageType: 'proauto-report' },
        },
    });

    it('должен отправить цели при покупке одного отчёта', () => {
        const purchaseCount = 1;

        return store.dispatch(sendMarketingEventByReport(purchaseCount, paymentModalResult)).then(() => {
            expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith('CARS_OPEN_REPORT_HISTORY');
            expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith('PURCHASE_SINGLE_REPORT_HISTORY');
            expect(marketingMock.sendEvents).toHaveBeenCalledWith(paymentModalResult,
                [
                    { counter: 'adwords', type: 'conversionBuyOneReport' },
                ],
            );
            expect(marketingMock.sendEvents).toHaveBeenCalledWith(paymentModalResult,
                [
                    { counter: 'adwords', type: 'buyReportInHistory' },
                    { counter: 'adwords', type: 'conversionBuyReportDV360' },
                    { counter: 'adwords', type: 'conversionBuyReportCM360' },
                    { counter: 'adwords', type: 'conversionBuyReportCM360TAG' },
                ],
            );
        });
    });

    it('должен отправить цели при покупке пакета из 5 отчётов', () => {
        const purchaseCount = 5;

        return store.dispatch(sendMarketingEventByReport(purchaseCount, paymentModalResult)).then(() => {
            expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith('CARS_OPEN_REPORT_HISTORY');
            expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith('PURCHASE_BUNDLE-5_REPORT_HISTORY');
            expect(marketingMock.sendEvents).toHaveBeenCalledWith(paymentModalResult,
                [
                    { counter: 'adwords', type: 'conversionBuyFiveReports' },
                ],
            );
            expect(marketingMock.sendEvents).toHaveBeenCalledWith(paymentModalResult,
                [
                    { counter: 'adwords', type: 'buyReportPackageInHistory' },
                    { counter: 'adwords', type: 'conversionBuyReportDV360' },
                    { counter: 'adwords', type: 'conversionBuyReportCM360' },
                    { counter: 'adwords', type: 'conversionBuyReportCM360TAG' },
                ],
            );
        });
    });

    it('должен отправить цели при покупке пакета отчётов', () => {
        const purchaseCount = 10;

        return store.dispatch(sendMarketingEventByReport(purchaseCount, paymentModalResult)).then(() => {
            expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith('CARS_OPEN_REPORT_HISTORY');
            expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith('PURCHASE_BUNDLE_REPORT_HISTORY');
            expect(marketingMock.sendEvents).toHaveBeenCalledWith(paymentModalResult,
                [
                    { counter: 'adwords', type: 'conversionBuyTenReports' },
                ],
            );
            expect(marketingMock.sendEvents).toHaveBeenCalledWith(paymentModalResult,
                [
                    { counter: 'adwords', type: 'buyReportPackageInHistory' },
                    { counter: 'adwords', type: 'conversionBuyReportDV360' },
                    { counter: 'adwords', type: 'conversionBuyReportCM360' },
                    { counter: 'adwords', type: 'conversionBuyReportCM360TAG' },
                ],
            );
        });
    });

    it('если для просмотра отчёта расходуется пакет, не должен отправлять метрики о покупке', () => {
        const purchaseCount = null;

        return store.dispatch(sendMarketingEventByReport(purchaseCount, paymentModalResult)).then(() => {
            expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith('CARS_OPEN_REPORT_HISTORY');
            expect(contextMock.metrika.reachGoal).not.toHaveBeenCalledWith('PURCHASE_SINGLE_REPORT_HISTORY');
            expect(contextMock.metrika.reachGoal).not.toHaveBeenCalledWith('PURCHASE_BUNDLE_REPORT_HISTORY');
            expect(marketingMock.sendEvents).not.toHaveBeenCalled();
        });
    });
});

describe('лендинг проавто', () => {
    const store = mockStore({
        config: {
            data: { pageType: 'proauto-landing' },
        },
    });

    it('должен отправить цели при покупке пакета из 5 отчётов', () => {
        const purchaseCount = 5;

        return store.dispatch(sendMarketingEventByReport(purchaseCount, paymentModalResult)).then(() => {
            expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith('PURCHASE_BUNDLE-5_REPORT_HISTORY');
            expect(marketingMock.sendEvents).toHaveBeenCalledWith(paymentModalResult,
                [
                    { counter: 'adwords', type: 'conversionBuyFiveReports' },
                ],
            );
            expect(marketingMock.sendEvents).toHaveBeenCalledWith(paymentModalResult,
                [
                    { counter: 'adwords', type: 'buyReportPackageInHistory' },
                    { counter: 'adwords', type: 'conversionBuyReportDV360' },
                    { counter: 'adwords', type: 'conversionBuyReportCM360' },
                    { counter: 'adwords', type: 'conversionBuyReportCM360TAG' },
                ],
            );
        });
    });

    it('должен отправить цели при покупке пакета отчётов', () => {
        const purchaseCount = 10;

        return store.dispatch(sendMarketingEventByReport(purchaseCount, paymentModalResult)).then(() => {
            expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith('PURCHASE_BUNDLE_REPORT_HISTORY');
            expect(marketingMock.sendEvents).toHaveBeenCalledWith(paymentModalResult,
                [
                    { counter: 'adwords', type: 'conversionBuyTenReports' },
                ],
            );
            expect(marketingMock.sendEvents).toHaveBeenCalledWith(paymentModalResult,
                [
                    { counter: 'adwords', type: 'buyReportPackageInHistory' },
                    { counter: 'adwords', type: 'conversionBuyReportDV360' },
                    { counter: 'adwords', type: 'conversionBuyReportCM360' },
                    { counter: 'adwords', type: 'conversionBuyReportCM360TAG' },
                ],
            );
        });
    });

    it('если для просмотра отчёта расходуется пакет, не должен отправлять метрики о покупке', () => {
        const purchaseCount = null;

        return store.dispatch(sendMarketingEventByReport(purchaseCount, paymentModalResult)).then(() => {
            expect(contextMock.metrika.reachGoal).not.toHaveBeenCalledWith('PURCHASE_SINGLE_REPORT_HISTORY');
            expect(contextMock.metrika.reachGoal).not.toHaveBeenCalledWith('PURCHASE_BUNDLE_REPORT_HISTORY');
            expect(marketingMock.sendEvents).not.toHaveBeenCalled();
        });
    });
});

describe('кабинет перекупа оффера', () => {
    const store = mockStore({
        config: {
            data: { pageType: 'reseller-sales' },
        },
    });

    it('должен отправить цели при покупке одного отчёта', () => {
        const purchaseCount = 1;

        return store.dispatch(sendMarketingEventByReport(purchaseCount, paymentModalResult)).then(() => {
            expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith('CARS_OPEN_REPORT_CARD');
            expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith('PURCHASE_RESELLER_SINGLE_REPORT_CARD');
        });
    });

    it('должен отправить цели при покупке пакета из 5 отчётов', () => {
        const purchaseCount = 5;

        return store.dispatch(sendMarketingEventByReport(purchaseCount, paymentModalResult)).then(() => {
            expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith('CARS_OPEN_REPORT_CARD');
            expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith('PURCHASE_RESELLER_BUNDLE-5_REPORT_CARD');
        });
    });

    it('должен отправить цели при покупке пакета отчётов', () => {
        const purchaseCount = 10;

        return store.dispatch(sendMarketingEventByReport(purchaseCount, paymentModalResult)).then(() => {
            expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith('CARS_OPEN_REPORT_CARD');
            expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith('PURCHASE_RESELLER_BUNDLE_REPORT_CARD');
        });
    });

    it('если для просмотра отчёта расходуется пакет, не должен отправлять метрики о покупке', () => {
        const purchaseCount = null;

        return store.dispatch(sendMarketingEventByReport(purchaseCount, paymentModalResult)).then(() => {
            expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith('CARS_OPEN_REPORT_CARD');
            expect(contextMock.metrika.reachGoal).not.toHaveBeenCalledWith('PURCHASE_RESELLER_SINGLE_REPORT_CARD');
            expect(contextMock.metrika.reachGoal).not.toHaveBeenCalledWith('PURCHASE_RESELLER_BUNDLE_REPORT_CARD');
        });
    });
});
