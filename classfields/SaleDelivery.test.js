const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const SaleDelivery = require('./SaleDelivery');
const { noop } = require('lodash');
const contextMock = require('autoru-frontend/mocks/contextMock').default;

it('должен вернуть корректный компонент, если длина массива городов доставки в винительном падеже больше 1', () => {
    expect(shallowToJson(shallow(
        <SaleDelivery
            formattedDeliveryTo={ [ 'Москву', 'Балашиху' ] }
            showDeliverySettings={ noop }
        />,
        { context: contextMock },
    ))).toMatchSnapshot();
});

it('должен вернуть корректный компонент, если длина массива городов доставки в винительном падеже равна 1', () => {
    expect(shallowToJson(shallow(
        <SaleDelivery
            formattedDeliveryTo={ [ 'Балашиху' ] }
            showDeliverySettings={ noop }
        />,
        { context: contextMock },
    ))).toMatchSnapshot();
});

it('должен вернуть корректный компонент, если длина массива городов доставки в винительном падеже равна 0', () => {
    expect(shallowToJson(shallow(
        <SaleDelivery
            formattedDeliveryTo={ [] }
            showDeliverySettings={ noop }
        />,
        { context: contextMock },
    ))).toMatchSnapshot();
});

it('должен вернуть корректный задизейбленный компонент', () => {
    expect(shallowToJson(shallow(
        <SaleDelivery
            formattedDeliveryTo={ [ 'Балашиху' ] }
            showDeliverySettings={ noop }
            disabled={ true }
        />,
        { context: contextMock },
    ))).toMatchSnapshot();
});

it('должен вызвать showDeliverySettings и отправить событие в метрику, ' +
    'если disabled = false', () => {
    const showDeliverySettings = jest.fn();
    const deliverySettingsInstance = shallow(
        <SaleDelivery
            formattedDeliveryTo={ [ 'Балашиху' ] }
            showDeliverySettings={ showDeliverySettings }
            disabled={ false }
            category="cars"
            section="used"
            isCarCategory={ true }
        />,
        { context: contextMock },
    ).instance();

    deliverySettingsInstance.onClick();
    expect(showDeliverySettings).toHaveBeenCalled();
    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'delivery', 'show_popup', 'cars', 'used' ]);
});

it('не должен вызвать showDeliverySettings, если disabled = true', () => {
    const showDeliverySettings = jest.fn();
    const deliverySettingsInstance = shallow(
        <SaleDelivery
            formattedDeliveryTo={ [ 'Балашиху' ] }
            showDeliverySettings={ showDeliverySettings }
            disabled={ true }
        />,
        { context: contextMock },
    ).instance();

    deliverySettingsInstance.onClick();
    expect(showDeliverySettings).not.toHaveBeenCalled();
});
