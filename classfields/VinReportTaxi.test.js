const React = require('react');
const { shallow } = require('enzyme');
const _ = require('lodash');

const vinReportMock = require('auto-core/react/dataDomain/defaultVinReport/mocks/defaultVinReport.mock');

const VinReportTaxi = require('./VinReportTaxi');

const DEFAULT_HEADER = _.cloneDeep(vinReportMock.data.report.taxi.header);
delete DEFAULT_HEADER.is_updating;

it('должен правильно сформировать тайтл записи, если есть обе даты', () => {
    const vinReportTaxiMock = {
        header: DEFAULT_HEADER,
        taxi_records: [
            {
                license_from: '1517191200000',
                license_to: '1674871200000',
                license: '111',
            },
        ],
    };
    const wrapper = shallow(
        <VinReportTaxi taxi={ vinReportTaxiMock }/>,
    );
    expect(wrapper.find('.VinReportTaxi__recordDates').text()).toEqual('29 января 2018 — 28 января 2023');

});

it('должен правильно сформировать тайтл записи, если есть только дата "от"', () => {
    const vinReportTaxiMock = {
        header: DEFAULT_HEADER,
        taxi_records: [
            {
                license_from: '1517191200000',
                license: '111',
            },
        ],
    };
    const wrapper = shallow(
        <VinReportTaxi taxi={ vinReportTaxiMock }/>,
    );
    expect(wrapper.find('.VinReportTaxi__recordDates').text()).toEqual('29 января 2018 — настоящее время');

});

it('должен правильно сформировать тайтл записи, если есть только дата до', () => {
    const vinReportTaxiMock = {
        header: DEFAULT_HEADER,
        taxi_records: [
            {
                license_to: '1674871200000',
                license: '111',
            },
        ],
    };
    const wrapper = shallow(
        <VinReportTaxi taxi={ vinReportTaxiMock }/>,
    );
    expect(wrapper.find('.VinReportTaxi__recordDates').text()).toEqual('—');
});

it('должен правильно сформировать тайтл записи, если нет дат вообще', () => {
    const vinReportTaxiMock = {
        header: DEFAULT_HEADER,
        taxi_records: [
            {
                license: '111',
            },
        ],
    };
    const wrapper = shallow(
        <VinReportTaxi taxi={ vinReportTaxiMock }/>,
    );
    expect(wrapper.find('.VinReportTaxi__recordDates').text()).toEqual('—');

});

it('VinReportTaxi должен отрендерить VinReportLoading, если is_updating и нет данных', () => {
    const vinReportTaxiMock = {
        header: {
            ...DEFAULT_HEADER,
            is_updating: true,
        },
    };
    const wrapper = shallow(
        <VinReportTaxi taxi={ vinReportTaxiMock }/>,
    );

    expect(wrapper.find('VinReportLoading').exists()).toBe(true);
});
