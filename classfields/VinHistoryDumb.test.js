/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/dataDomain/vinReport/helpers/reportPaidNow', () => jest.fn());

const React = require('react');
const { shallow } = require('enzyme');
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const _ = require('lodash');

const vinReportMock = require('auto-core/react/dataDomain/vinReport/mocks/freeReport-mercedes.mock');
const reportPaidNow = require('auto-core/react/dataDomain/vinReport/helpers/reportPaidNow');

const VinHistoryDumb = require('./VinHistoryDumb');

describe('vinReport', () => {
    it('должен вызвать загрузку отчёта, если он еще не загружен', () => {
        const fetchVinReport = jest.fn();
        shallow(
            <VinHistoryDumb
                isAuth={ true }
                fetchVinReport={ fetchVinReport }
                vinReport={{ ...vinReportMock, fetched: false, fetching: true }}
                vinReportParams="vin=foo"
                vinReportPaymentParams="vin=foo"
            />,
            { context: contextMock },
        );
        expect(fetchVinReport).toHaveBeenCalledTimes(1);
    });

    it('не должен вызвать загрузку отчёта, если он уже загружен', () => {
        const fetchVinReport = jest.fn();

        shallow(
            <VinHistoryDumb
                isAuth={ true }
                isCardHistory={ true }
                fetchVinReport={ fetchVinReport }
                vinReport={ vinReportMock }
                vinReportParams="vin=foo"
                vinReportPaymentParams="vin=foo"
            />,
            { context: contextMock },
        );
        expect(fetchVinReport).toHaveBeenCalledTimes(0);
    });

    it('должен перезапросить отчёт при смене параметров отчёта', () => {
        const fetchVinReport = jest.fn();
        const wrapper = shallow(
            <VinHistoryDumb
                isAuth={ false }
                isCardHistory={ true }
                fetchVinReport={ fetchVinReport }
                vinReport={ vinReportMock }
                vinReportParams="vin=foo"
                vinReportPaymentParams="vin=foo"
            />,
            { context: contextMock },
        );
        wrapper.setProps({ vinReportParams: 'vin=bar' });
        expect(fetchVinReport).toHaveBeenCalledTimes(1);
    });

    it('должен отправить метрики при показе полной версии отчёта после оплаты на странице отчёта', () => {
        const sendMarketingEventByReportMock = jest.fn(() => Promise.resolve());
        const freeReportMock = vinReportMock;
        const paidREportMock = _.cloneDeep(vinReportMock);
        paidREportMock.report.report_type = 'PAID_REPORT';

        reportPaidNow.mockImplementation(() => true);

        const wrapper = shallow(
            <VinHistoryDumb
                authModal={{}}
                isAuth={ true }
                isCardHistory={ false }
                fetchVinReport={ jest.fn() }
                sendMarketingEventByReport={ sendMarketingEventByReportMock }
                vinReport={ freeReportMock }
                vinReportParams="vin=foo"
                vinReportPaymentParams="vin=foo"
            />,
            { context: contextMock },
        );
        wrapper.setProps({ vinReportParams: 'vin=foo', vinReport: paidREportMock });

        expect(sendMarketingEventByReportMock).toHaveBeenCalledTimes(1);
    });

    it('должен редиректнуть на страницу отчета с карточки', async() => {
        const sendMarketingEventByReportMock = jest.fn(() => Promise.resolve());
        const link = jest.fn();
        const freeReportMock = vinReportMock;
        const paidREportMock = _.cloneDeep(vinReportMock);
        paidREportMock.report.report_type = 'PAID_REPORT';

        reportPaidNow.mockImplementation(() => true);

        const wrapper = shallow(
            <VinHistoryDumb
                authModal={{}}
                isAuth={ true }
                isCardHistory={ true }
                fetchVinReport={ jest.fn() }
                sendMarketingEventByReport={ sendMarketingEventByReportMock }
                vinReport={ freeReportMock }
                vinReportParams="vin=foo"
                vinReportPaymentParams="vin=foo"
            />,
            { context: { ...contextMock, link } },
        );
        expect(link).toHaveBeenCalledTimes(0);
        wrapper.setProps({ vinReportParams: 'vin=foo', vinReport: paidREportMock });

        expect(sendMarketingEventByReportMock).toHaveBeenCalledTimes(1);

        await sendMarketingEventByReportMock();
        expect(link).toHaveBeenCalledTimes(1);

    });

    it('должен перезапросить отчёт после обычного логина без попытки покупки', () => {
        const fetchVinReport = jest.fn();
        const wrapper = shallow(
            <VinHistoryDumb
                isAuth={ false }
                isCardHistory={ true }
                fetchVinReport={ fetchVinReport }
                vinReport={ vinReportMock }
                vinReportParams='{"vin":"foo"}'
                vinReportPaymentParams='{"vin":"foo"}'
            />,
            { context: contextMock },
        );
        wrapper.setProps({ isAuth: true });
        expect(fetchVinReport).toHaveBeenCalledTimes(1);
        expect(fetchVinReport).toHaveBeenCalledWith('{"vin":"foo"}', '{"vin":"foo"}', undefined, {});
    });

    it('должен перезапросить отчёт после логина с кликом по "купить" с попыткой покупки', () => {
        const fetchVinReport = jest.fn();
        const wrapper = shallow(
            <VinHistoryDumb
                isAuth={ false }
                isCardHistory={ true }
                fetchVinReport={ fetchVinReport }
                vinReport={ vinReportMock }
                vinReportParams='{"vin":"foo"}'
                vinReportPaymentParams='{"vin":"foo"}'
                tryToBuyReport={ true }
            />,
            { context: contextMock },
        );
        wrapper.setProps({ isAuth: true });
        expect(fetchVinReport).toHaveBeenCalledTimes(1);
        expect(fetchVinReport).toHaveBeenCalledWith('{"vin":"foo"}', '{"vin":"foo"}', true, {});
    });

    it('должен перезапросить отчёт после логина с кликом по "купить по квоте" с попыткой покупки', () => {
        const fetchVinReport = jest.fn();
        const wrapper = shallow(
            <VinHistoryDumb
                isAuth={ false }
                isCardHistory={ true }
                fetchVinReport={ fetchVinReport }
                vinReport={ vinReportMock }
                vinReportParams='{"vin":"foo"}'
                vinReportPaymentParams='{"vin":"foo"}'
                buyVinReportWithQuota={ true }
            />,
            { context: contextMock },
        );
        wrapper.setProps({ isAuth: true });
        expect(fetchVinReport).toHaveBeenCalledTimes(1);
        expect(fetchVinReport).toHaveBeenCalledWith('{"vin":"foo"}', '{"vin":"foo"}', true, {});
    });

    it('должен вызвать загрузку отчёта с try-to-buy при смене пропов', () => {
        const fetchVinReport = jest.fn();
        const wrapper = shallow(
            <VinHistoryDumb
                isAuth={ true }
                isCardHistory={ true }
                fetchVinReport={ fetchVinReport }
                vinReport={ vinReportMock }
                vinReportParams="vin=foo"
                vinReportPaymentParams="vin=foo"
            />,
            { context: contextMock },
        );
        wrapper.setProps({ buyVinReportWithQuota: true });
        expect(fetchVinReport).toHaveBeenCalledTimes(1);
        // параметр try-to-buy должен быть true
        expect(fetchVinReport.mock.calls[0][2]).toBe(true);
    });

    it('должен вызвать загрузку отчёта с try-to-buy после успешной оплаты', () => {
        const fetchVinReport = jest.fn();
        const wrapper = shallow(
            <VinHistoryDumb
                isAuth={ true }
                isCardHistory={ true }
                fetchVinReport={ fetchVinReport }
                vinReport={ vinReportMock }
                vinReportParams="vin=foo"
                vinReportPaymentParams="vin=foo"
            />,
            { context: contextMock },
        );
        wrapper.setProps({ paymentModalResult: { timestamp: 123, services: [ 'offers-history-reports' ] } });
        expect(fetchVinReport).toHaveBeenCalledTimes(1);
        // параметр try-to-buy должен быть true
        expect(fetchVinReport.mock.calls[0][2]).toBe(true);
    });

    it('должен отправить метрику и средиректить на стэндэлоун после оплаты', () => {
        const mockPromise = Promise.resolve();
        const sendMarketingEventByReportMock = jest.fn(() => mockPromise);
        const fetchVinReport = jest.fn();

        const vinReportPaidMock = _.cloneDeep(vinReportMock);
        vinReportPaidMock.report.report_type = 'PAID_REPORT';

        const wrapper = shallow(
            <VinHistoryDumb
                authModal={{ isOpened: false }}
                isAuth={ true }
                isCardHistory={ true }
                fetchVinReport={ fetchVinReport }
                sendMarketingEventByReport={ sendMarketingEventByReportMock }
                tryToBuyReport={ true }
                vinReport={ vinReportMock }
                vinReportParams="vin=foo"
                vinReportPaymentParams="vin=foo"
            />,
            { context: contextMock },
        );
        wrapper.setProps({ vinReport: vinReportPaidMock });

        return mockPromise
            .then(() => {
                expect(sendMarketingEventByReportMock).toHaveBeenCalledTimes(1);
                expect(window.location.assign.mock.calls[0]).toEqual([ 'link/proauto-report/?history_entity_id=1084368429-e9a4c888' ]);

                delete global.fetch;
            });
    });
});

describe('отправляет правильные метрики об оплате при покупке отчета', () => {
    let context;
    let vinReport;
    let updatedProps;

    const shallowRenderReport = (props = {}) => {
        const wrapper = shallow(
            <VinHistoryDumb
                isAuth={ true }
                isOwner={ props.isOwner }
                isCardHistory={ props.isCardHistory }
                fetchVinReport={ jest.fn() }
                vinReport={ vinReport }
                vinReportParams="vin=foo"
                vinReportPaymentParams="vin=foo"
                authModal={{}}
                tryToBuyReport={ true }
                sendMarketingEventByReport={ () => Promise.reject() }
                paymentModalResult={ props.paymentModalResult }
                handleVinReportPurchase={ () => null }
            />,
            { context },
        );

        wrapper.setProps(updatedProps);
    };

    beforeEach(() => {
        context = _.cloneDeep(contextMock);
        vinReport = { ...vinReportMock, fetched: true, fetching: false };
        updatedProps = { vinReport: _.cloneDeep(vinReport) };
        updatedProps.vinReport.report.report_type = 'PAID_REPORT';
    });

    it('стендалоун', () => {
        shallowRenderReport();

        const expectedResult = [ 'history_purchase', 'payment_success', 'money', 'single_report', 'not_owner' ];

        expect(context.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
        expect(context.metrika.sendPageEvent.mock.calls[0][0]).toEqual(expectedResult);
    });

    it('на карточке', () => {
        shallowRenderReport({
            isCardHistory: true,
        });

        const expectedResult = [ 'history_report', 'payment_success', 'money', 'single_report', 'not_owner' ];

        expect(context.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
        expect(context.metrika.sendPageEvent.mock.calls[0][0]).toEqual(expectedResult);
    });

    it('по квоте', () => {
        vinReport.billing = { quota_left: 1 };

        shallowRenderReport({
            isCardHistory: true,
        });

        const expectedResult = [ 'history_report', 'payment_success', 'quota', 'single_report', 'not_owner' ];

        expect(context.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
        expect(context.metrika.sendPageEvent.mock.calls[0][0]).toEqual(expectedResult);
    });

    it('куплен пакет', () => {
        shallowRenderReport({
            isCardHistory: true,
            paymentModalResult: {
                paymentParams: {
                    purchaseCount: 10,
                },
            },
        });

        const expectedResult = [ 'history_report', 'payment_success', 'money', 'bundle', 'not_owner' ];

        expect(context.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
        expect(context.metrika.sendPageEvent.mock.calls[0][0]).toEqual(expectedResult);
    });

    it('на карточке владельцем объявления', () => {
        shallowRenderReport({
            isCardHistory: true,
            isOwner: true,
        });

        const expectedResult = [ 'history_report', 'payment_success', 'money', 'single_report', 'owner' ];

        expect(context.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
        expect(context.metrika.sendPageEvent.mock.calls[0][0]).toEqual(expectedResult);
    });
});
