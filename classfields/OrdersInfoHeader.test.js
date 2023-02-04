const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const OrdersInfoHeader = require('./OrdersInfoHeader');

it('должен рендерить только свитчер новых, если по свитчеру б/у нет данных', () => {
    const tree = shallow(
        <OrdersInfoHeader
            canWrite={ true }
            newCarsSwitcher={{
                cost: 500,
                title: 'Легковые новые',
                isActive: true,
            }}
        />,
    );

    const controls = tree.find('.OrdersInfoHeader__controls__inner');

    expect(shallowToJson(controls)).toMatchSnapshot();
});

it('не должен рендерить свитчер б/у, если по свитчеру новых нет данных', () => {
    const tree = shallow(
        <OrdersInfoHeader
            canWrite={ true }
            usedCarsSwitcher={{
                cost: 500,
                title: 'Легковые с пробегом',
                isActive: true,
            }}
        />,
    );

    const controls = tree.find('.OrdersInfoHeader__controls__inner');

    expect(shallowToJson(controls)).toMatchSnapshot();
});

it('не должен рендерить свитчеры, если по ним нет данных', () => {
    const tree = shallow(
        <OrdersInfoHeader
            canWrite={ true }
        />,
    );

    const controls = tree.instance().renderControls();

    expect(controls).toBeNull();
});
