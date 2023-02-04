const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const DeliverySettingsRegion = require('./DeliverySettingsRegion');
const { noop } = require('lodash');

it('должен вернуть корректный компонент, если deleted = true', () => {
    expect(
        shallowToJson(
            shallow(
                <DeliverySettingsRegion
                    region={{
                        id: '111',
                        name: 'some name',
                        address: 'some address',
                        products: [ { title: 'delivery', price: 200 } ],
                        deleted: true,
                    }}
                    priceTexts={{ delivery: { short_name: 'доставка' } }}
                    restoreRegion={ noop }
                    deleteRegion={ noop }
                />),
        ))
        .toMatchSnapshot();
});

it('должен вернуть корректный компонент, если deleted = false', () => {
    expect(
        shallowToJson(
            shallow(
                <DeliverySettingsRegion
                    region={{
                        id: '222',
                        name: 'some name',
                        address: 'some address',
                        products: [ { title: 'delivery', price: 200 } ],
                        deleted: false,
                        expand: false,
                    }}
                    priceTexts={{ delivery: { short_name: 'доставка' } }}
                    restoreRegion={ noop }
                    deleteRegion={ noop }
                />),
        ))
        .toMatchSnapshot();
});

it('toggleExpanded: должен установить кооректный state', () => {
    const DeliverySettingsRegionInstance = shallow(
        <DeliverySettingsRegion
            region={{
                id: '222',
                name: 'some name',
                address: 'some address',
                products: [ { title: 'delivery', price: 200 } ],
                deleted: false,
            }}
            priceTexts={{ delivery: { short_name: 'доставка' } }}
            restoreRegion={ noop }
            deleteRegion={ noop }
        />).instance();

    DeliverySettingsRegionInstance.state = { expanded: true };
    DeliverySettingsRegionInstance.toggleExpanded();
    expect(DeliverySettingsRegionInstance.state).toEqual({ expanded: false });
});
