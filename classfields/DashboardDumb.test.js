const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const _ = require('lodash');

const contextMock = require('autoru-frontend/mocks/contextMock').default;
const widgetProps = require('./mocks/widgetProps');

const DashboardDumb = require('./DashboardDumb');

it('должен рендерить правильный набор виджетов для доступного волк-ина', () => {
    expect(shallowToJson(
        shallow(
            <DashboardDumb
                { ...widgetProps }
            />,
            { context: contextMock },
        ))).toMatchSnapshot();
});

it('должен рендерить набор виджетов, если волк-ин недоступен', () => {
    const propsClone = _.cloneDeep(widgetProps);

    propsClone.walkIn.shouldShow = false;

    expect(shallowToJson(
        shallow(
            <DashboardDumb
                { ...propsClone }
            />,
            { context: contextMock },
        ))).toMatchSnapshot();
});

it('не должен рендерить BalanceWidget, если balanceWidget.shouldShow = false', () => {
    const propsClone = _.cloneDeep(widgetProps);

    propsClone.balanceWidget.shouldShow = false;
    expect(shallow(
        <DashboardDumb
            { ...propsClone }
        />,
        { context: contextMock },
    ).find('DashboardBalanceWidget')).toHaveLength(0);
});

it('не должен рендерить DashboardMySalesWidget, если mySales.shouldShow = false', () => {
    expect(shallow(
        <DashboardDumb
            { ...widgetProps }
            mySales={{
                shouldShow: false,
                total: 30,
                dropdownItems: [],
                chartData: {},
            }}
        />,
        { context: contextMock },
    ).find('DashboardMySalesWidget')).toHaveLength(0);
});

it('не должен рендерить DashboardPromoFeaturesWidget, если promoFeatures.hasFeatures = false', () => {
    expect(shallow(
        <DashboardDumb
            { ...widgetProps }
            promoFeatures={{
                hasFeatures: false,
            }}
        />,
        { context: contextMock },
    ).find('DashboardPromoFeaturesWidget')).toHaveLength(0);
});

it('не должен рендерить DashboardPromoCallsWidget, если calls.shouldShow = false', () => {
    expect(shallow(
        <DashboardDumb
            { ...widgetProps }
            calls={{
                shouldShow: false,
            }}
        />,
        { context: contextMock },
    ).find('DashboardCallsWidget')).toHaveLength(0);
});

it('не должен рендерить DashboardWalkInWidget, если walkIn.shouldShow = false', () => {
    expect(shallow(
        <DashboardDumb
            { ...widgetProps }
            walkIn={{
                shouldShow: false,
            }}
        />,
        { context: contextMock },
    ).find('DashboardWalkInWidget')).toHaveLength(0);
});

it('не должен рендерить DashboardWalletWidget, если wallet.shouldShow = false', () => {
    expect(shallow(
        <DashboardDumb
            { ...widgetProps }
            wallet={{
                shouldShow: false,
            }}
        />,
        { context: contextMock },
    ).find('DashboardWalletWidget')).toHaveLength(0);
});
