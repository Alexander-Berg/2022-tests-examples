const React = require('react');
const _ = require('lodash');
const { shallow } = require('enzyme');

const VinReportHistoryEstimate = require('./VinReportHistoryEstimate').default;

const RECORD = {
    region_name: 'Москва',
    date: '1504667600000',
    mileage: 100,
    mileage_status: 'OK',
    partner_name: 'FRESH',
    partner_url: 'https://aaaa.bb',
};

it('должен отрисовать все поля, включая ссылку, в VinReportHistoryEstimate', () => {
    const wrapper = shallow(
        <VinReportHistoryEstimate record={ RECORD }/>,
    );

    expect(wrapper.dive().find('Link')).toHaveLength(1);
    expect(wrapper.dive().find('.VinReportHistoryRecord__row')).toHaveLength(3);
});

it('должен отрисовать источник без ссылки в VinReportHistoryEstimate', () => {
    const record = _.cloneDeep(RECORD);
    delete record.partner_url;

    const wrapper = shallow(
        <VinReportHistoryEstimate record={ record }/>,
    );

    expect(wrapper.dive().find('Link')).not.toExist();
    expect(wrapper.dive().find('.VinReportHistoryRecord__row')).toHaveLength(3);
});

it('не должен рисовать поле, если нет значения в VinReportHistoryEstimate', () => {
    const record = _.cloneDeep(RECORD);
    delete record.partner_name;
    delete record.region_name;

    const wrapper = shallow(
        <VinReportHistoryEstimate record={ record }/>,
    );

    expect(wrapper.dive().find('Link')).not.toExist();
    expect(wrapper.dive().find('.VinReportHistoryRecord__row')).toHaveLength(1);
});
