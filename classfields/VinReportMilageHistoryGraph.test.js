/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

require('jest-enzyme');
const React = require('react');
const { mount } = require('enzyme');

const VinReportMilageHistoryGraph = require('./VinReportMilageHistoryGraph');

const PROPS = {
    graphData: [
        { date: '2007-03-08T21:00:00Z', mileage: 0 },
        { date: '2008-07-04T20:00:00Z', mileage: 1000 },
        { date: '2020-09-11T21:00:00Z', mileage: 1700 },
    ],
    onBulletMouseOver: jest.fn(),
    onBulletMouseLeave: jest.fn(),
    chartYHeight: 174,
    chartPaddingTop: 0,
    isMobile: false,
};

it('не должен рендерить цифры типа 1.799999999', () => {
    const wrapper = mount(<VinReportMilageHistoryGraph { ...PROPS }/>);
    const target = wrapper.find('.VinReportMilageHistoryGraph__gridItemValue').first();
    expect(target.text()).toEqual('1.8');
});
