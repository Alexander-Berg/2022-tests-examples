const React = require('react');
const { shallow } = require('enzyme');

const VinReportRepairCalculations = require('./VinReportRepairCalculations');

it('VinReportRepairCalculations должен отрендерить VinReportLoading, если is_updating и нет данных', () => {
    const header = {
        title: 'Рассчет стоимости ремонта',
        timestamp_update: '1571028005586',
        is_updating: true,
    };

    const wrapper = shallow(
        <VinReportRepairCalculations repair={{ header }}/>,
    );

    expect(wrapper.find('VinReportLoading').exists()).toBe(true);
});
