const React = require('react');

const { shallow } = require('enzyme');

const VinReportHistoryDTP = require('./VinReportHistoryDTP');

it('должен отрисовать дату и город, если они есть', () => {
    const wrapper = shallow(
        <VinReportHistoryDTP
            record={{
                timestamp: '1556355600000',
                title: 'Столкновение',
                place: 'Москва',
            }}/>,
    );

    expect(wrapper.find('VinReportHistoryRecord').prop('date')).toEqual('27 апреля 2019, Москва');
});

it('должен отрисовать только дату, если нет города', () => {
    const wrapper = shallow(
        <VinReportHistoryDTP
            record={{
                timestamp: '1556355600000',
                title: 'Столкновение',
            }}/>,
    );

    expect(wrapper.find('VinReportHistoryRecord').prop('date')).toEqual('27 апреля 2019');
});

it('должен передать отрисованную схему, если есть повреждения', () => {
    const wrapper = shallow(
        <VinReportHistoryDTP
            record={{
                damages: [],
                timestamp: '1556355600000',
                title: 'Столкновение',
            }}/>,
    );

    expect(wrapper.find('VinReportHistoryRecord').prop('children')).toBeDefined();
});

it('если нет повреждений, не должен рисовать схему', () => {
    const wrapper = shallow(
        <VinReportHistoryDTP
            record={{
                timestamp: '1556355600000',
                title: 'Столкновение',
            }}/>,
    );

    expect(wrapper.find('VinReportHistoryRecord').prop('children')).toEqual([ null, null ]);
});
