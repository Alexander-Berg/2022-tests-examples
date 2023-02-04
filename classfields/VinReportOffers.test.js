const React = require('react');
const { shallow } = require('enzyme');

const VinReportOffers = require('./VinReportOffers').default;

it('не должен отрисовать объявления, если нет записей', () => {
    const wrapper = shallow(
        <VinReportOffers/>,
    );

    expect(wrapper.type()).toBeNull();
});

it('VinReportOffers должен отрендерить VinReportLoading, если is_updating и нет данных', () => {
    const header = {
        title: 'Есть объявления',
        is_updating: true,
    };

    const wrapper = shallow(
        <VinReportOffers offersData={{ header }}/>,
    );

    expect(wrapper.find('VinReportLoading').exists()).toBe(true);
});
