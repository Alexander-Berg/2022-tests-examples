const React = require('react');
const { shallow } = require('enzyme');

const VinReportAuction = require('./VinReportAuction');

it('VinReportAuction должен отрендерить VinReportLoading, если is_updating и нет данных', () => {
    const header = {
        title: 'Продавался на аукционах битых автомобилей',
        timestamp_update: '1571028005586',
        is_updating: true,
    };

    const wrapper = shallow(
        <VinReportAuction auction={{ header }}/>,
    );

    expect(wrapper.find('VinReportLoading').exists()).toBe(true);
});
