const React = require('react');
const { shallow } = require('enzyme');

const VinReportDefaultBlock = require('./VinReportDefaultBlock').default;

const props = {
    data: {
        header: {
            timestamp_update: '1608719814815',
            title: 'Нахождение в розыске',
            is_updating: true,
        },
    },
    texts: {
        OK: 'Сведения о нахождении в розыске не обнаружены',
        ERROR: 'Автомобиль может находиться в розыске',
        UNKNOWN: 'Информация о розыске появится чуть позже',
    },
    label: 'По данным ГИБДД',
    id: 'wanted',
};

it('должен отрендерить VinReportDefaultBlock с лоадером, если нет данных и is_updating', async() => {
    const wrapper = shallow(
        <VinReportDefaultBlock
            { ...props }
        />,
    );

    expect(wrapper.find('VinReportLoading')).toExist();
});
