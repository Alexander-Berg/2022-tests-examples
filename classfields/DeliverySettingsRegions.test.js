const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const DeliverySettingsRegions = require('./DeliverySettingsRegions');
const { noop } = require('lodash');

it('должен вернуть корректный компонент', () => {
    const regions = [
        { deleted: false, products: [ { price: 10 }, { price: 30 } ], coord: { latitude: 1, longitude: 1 } },
        { deleted: true, products: [ { price: 20 }, { price: 20 } ], coord: { latitude: 2, longitude: 2 } },
        { deleted: false, products: [ { price: 30 }, { price: 10 } ], coord: { latitude: 3, longitude: 3 } },
    ];

    expect(
        shallowToJson(
            shallow(
                <DeliverySettingsRegions
                    regions={ regions }
                    expandRegionServices={ noop }
                    collapseRegionServices={ noop }
                    restoreRegion={ noop }
                    deleteRegion={ noop }
                />),
        ),
    ).toMatchSnapshot();
});
