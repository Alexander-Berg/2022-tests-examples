/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

const React = require('react');
const { shallow } = require('enzyme');
const _ = require('lodash');

const contextMock = require('autoru-frontend/mocks/contextMock').default;
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const configStateMock = require('auto-core/react/dataDomain/config/mock').default;
const vinReportMock = require('auto-core/react/dataDomain/defaultVinReport/mocks/defaultVinReport.mock');

const HistoryByVinDesktop = require('./HistoryByVinDesktop');

let state;
let context;

global.scroll = jest.fn();

beforeEach(() => {
    state = {
        bunker: {},
        config: configStateMock.value(),
        card: {},
        user: { data: { } },
        state: { authModal: {} },
    };

    context = _.cloneDeep(contextMock);
});

it('не должен выполнять поиск по vin, когда показываем отчёт по этому же vin', () => {
    const store = mockStore(state);
    const wrapper = shallow(
        <HistoryByVinDesktop params={{ vin_or_license_plate: 'A111AA11' }}/>, { context: { ...contextMock, store } },
    ).dive();

    wrapper.find('Connect(VinCheckSnippetMini)').simulate('inputSubmit', 'A111AA11');
    expect(store.getActions()).toEqual([]);
});

describe('метрики', () => {
    it('отправляем метрику ошибки', () => {
        const store = mockStore(state);
        const wrapper = shallow(
            <HistoryByVinDesktop/>, { context: { ...context, store } },
        ).dive();

        const errorParams = {
            vinReport: { error: {} },
            pageParams: { vin_or_license_plate: 'A111AA11' },
        };

        wrapper.setProps(errorParams);

        expect(context.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
        expect(context.metrika.sendPageEvent.mock.calls[0][0]).toEqual([ 'landing', 'search_error' ]);
    });

    it('отправляем метрику успешной загрузки превью', () => {
        const store = mockStore(state);
        const wrapper = shallow(
            <HistoryByVinDesktop/>, { context: { ...context, store } },
        ).dive();

        const props = {
            vinReport: { report: { report_type: 'FREE_REPORT' } },
            pageParams: { vin_or_license_plate: 'A111AA11' },
        };

        wrapper.setProps(props);

        expect(context.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
        expect(context.metrika.sendPageEvent.mock.calls[0][0]).toEqual([ 'history_purchase', 'view_preview' ]);
    });

    it('отправляем метрику успешной загрузки полного отчета', () => {
        const store = mockStore(state);
        const wrapper = shallow(
            <HistoryByVinDesktop/>, { context: { ...context, store } },
        ).dive();

        const props = {
            vinReport: _.cloneDeep(vinReportMock.data),
            pageParams: { vin_or_license_plate: 'A111AA11' },
        };

        wrapper.setProps(props);

        expect(context.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
        expect(context.metrika.sendPageEvent.mock.calls[0][0]).toEqual([ 'report', 'view', 'not_owner' ]);
    });

    it('отправляет метрику поиска', () => {
        const store = mockStore(state);
        const wrapper = shallow(
            <HistoryByVinDesktop/>, { context: { ...context, store } },
        ).dive();

        const props = {
            vinReport: _.cloneDeep(vinReportMock.data),
            pageParams: { vin_or_license_plate: 'A111AA11' },
        };

        wrapper.setProps(props);
        wrapper.find('Connect(VinCheckSnippetMini)').simulate('inputSubmit', 'B111BB11');

        expect(context.metrika.sendPageEvent).toHaveBeenCalledTimes(2);
        expect(context.metrika.sendPageEvent.mock.calls[1][0]).toEqual([ 'landing', 'perform_search' ]);
    });

    it('отправляет метрику скачивания PDF', () => {
        const store = mockStore(state);
        const wrapper = shallow(
            <HistoryByVinDesktop/>, { context: { ...context, store } },
        ).dive();

        const props = {
            vinReport: _.cloneDeep(vinReportMock.data),
            pageParams: { vin_or_license_plate: 'A111AA11' },
        };

        wrapper.setProps(props);
        wrapper.find('Connect(VinHistoryDumb)').dive().dive().find('Connect(VinReportDownload)').simulate('click');

        expect(context.metrika.sendPageEvent).toHaveBeenCalledTimes(2);
        expect(context.metrika.sendPageEvent.mock.calls[1][0]).toEqual([ 'report', 'download_pdf' ]);
    });

    it('страница прокручивается вверх, если нажать кнопку "Проверить", если показываем отчет по другому vin', () => {
        const store = mockStore(state);
        const wrapper = shallow(
            <HistoryByVinDesktop params={{ vin_or_license_plate: 'A111AA11' }}/>, { context: { ...contextMock, store } },
        ).dive();

        const windowSpy = jest.spyOn(window, 'scroll');

        wrapper.find('Connect(VinCheckSnippetMini)').simulate('inputSubmit', 'A333AA33');

        expect(windowSpy).toHaveBeenCalled();
        expect(windowSpy).toHaveBeenCalledWith(0, 0);
    });

    it('страница прокручивается вверх, если нажать кнопку "Проверить", если показываем отчет по такому же vin', () => {
        const store = mockStore(state);
        const wrapper = shallow(
            <HistoryByVinDesktop params={{ vin_or_license_plate: 'A111AA11' }}/>, { context: { ...contextMock, store } },
        ).dive();

        const windowSpy = jest.spyOn(window, 'scroll');

        wrapper.find('Connect(VinCheckSnippetMini)').simulate('inputSubmit', 'A111AA11');

        expect(windowSpy).toHaveBeenCalled();
        expect(windowSpy).toHaveBeenCalledWith(0, 0);
    });
});
