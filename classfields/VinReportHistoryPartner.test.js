const React = require('react');

const { shallow } = require('enzyme');

const VinReportHistoryPartner = require('./VinReportHistoryPartner');

const baseRecordMock = {
    timestamp: '1556355600000',
    mileage: 24000,
    partner_name: 'Данные техосмотра',
    diagnostic_card_text: '075540031908413',
    mileage_status: 'OK',
    valid_until_timestamp: '1582232400000',
};

it('должен отрисовать дату и место, если они есть', () => {
    const wrapper = shallow(
        <VinReportHistoryPartner
            record={{
                ...baseRecordMock,
                region_name: 'Москва',
            }}/>,
    );

    expect(wrapper.find('VinReportHistoryRecord').prop('date')).toEqual('27 апреля 2019, Москва');
});

it('должен отрисовать дату и место, если они есть и источник - анон', () => {
    const wrapper = shallow(
        <VinReportHistoryPartner
            record={{
                ...baseRecordMock,
                region_name: 'Москва',
                meta: { source: { is_anonymous: true } },
            }}/>,
    );

    expect(wrapper.find('VinReportHistoryRecord').prop('date')).toEqual('Апрель 2019, Москва');
});

it('должен отрисовать только дату, если нет места', () => {
    const wrapper = shallow(
        <VinReportHistoryPartner
            record={ baseRecordMock }/>,
    );

    expect(wrapper.find('VinReportHistoryRecord').prop('date')).toEqual('27 апреля 2019');
});
