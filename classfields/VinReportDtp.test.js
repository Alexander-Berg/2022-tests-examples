const React = require('react');
const { shallow } = require('enzyme');

const VinReportDtp = require('./VinReportDtp');

const dtpMock = {
    header: {
        title: 'Было дело',
        timestamp_update: '1571028005586',
        is_updating: true,
    },
    status: 'OK',
};

it('VinReportDtp рендерит VinReportLoading, если данные еще не пришли', async() => {
    const wrapper = shallow(
        <VinReportDtp dtp={ dtpMock }/>,
    );

    expect(wrapper.find('VinReportLoading').exists()).toBe(true);
});

it('VinReportDtp не должен отрендериться, если status NOT_VISIBLE', async() => {
    const wrapper = shallow(
        <VinReportDtp dtp={{ ...dtpMock, status: 'NOT_VISIBLE' }}/>,
    );

    expect(wrapper).toBeEmptyRender();
});
