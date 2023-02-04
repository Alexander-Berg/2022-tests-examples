/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/lib/event-log/statApi');

const React = require('react');
const { shallow } = require('enzyme');
const _ = require('lodash');

const VinPromoButton = require('./VinPromoButton');

const mockStore = require('autoru-frontend/mocks/mockStore').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const configStateMock = require('auto-core/react/dataDomain/config/mock').default;

const cardVinReportFree = require('auto-core/react/dataDomain/vinReport/mocks/freeReport-mercedes.mock');
const card = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');

const statApi = require('auto-core/lib/event-log/statApi').default;

const user = {
    data: {
        auth: true,
    },
};

it('не должен рендерить кнопку, если нет платного отчёта', () => {
    const vinReport = _.cloneDeep(cardVinReportFree);
    delete vinReport.billing;
    const wrapper = shallow(
        <VinPromoButton
            vinReport={ vinReport }
        />, { context: { ...contextMock, store: mockStore({ card, user: { data: {} } }) } },
    ).dive();
    expect(wrapper.isEmptyRender()).toBe(true);
});

describe('если платный отчет есть', () => {
    const context = _.cloneDeep(contextMock);
    const vinReport = _.cloneDeep(cardVinReportFree);
    const wrapper = shallow(
        <VinPromoButton
            vinReport={ vinReport }
        />, { context: { ...context, store: mockStore({ card, user }) } },
    ).dive();

    it('должен рендерить кнопку', () => {
        expect(wrapper.isEmptyRender()).toBe(false);
    });

    it('должен отправить метрику на клик', () => {
        wrapper.simulate('click');
        const expectedResult = [ 'history_report', 'click_vin_promo_button_sidebar', 'owner' ];
        expect(context.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
        expect(context.metrika.sendPageEvent.mock.calls[0][0]).toEqual(expectedResult);
    });
});

it('должен отправить событие paid_report_view_event во фронтлог', () => {
    const context = _.cloneDeep(contextMock);
    const vinReport = _.cloneDeep(cardVinReportFree);

    const wrapper = shallow(
        <VinPromoButton vinReport={ vinReport }/>,
        { context: { ...context, store: mockStore({
            config: configStateMock.withPageType('card').value(),
            card,
            user,
        }) } },
    ).dive();

    wrapper
        .find('.VinPromoButton').dive().dive().dive()
        .instance().onClick();

    expect(statApi.logImmediately).toHaveBeenCalledTimes(1);
    expect(statApi.logImmediately).toHaveBeenCalledWith({
        paid_report_view_event: {
            card_from: 'SERP',
            card_id: '1085562758-1970f439',
            category: 'CARS',
            context_block: 'BLOCK_CARD',
            context_page: 'PAGE_CARD',
            search_query_id: '',
            section: 'USED',
            self_type: 'TYPE_SINGLE',
            trade_in_allowed: false,
        },
    });
});
