/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const VendorColorSelector = require('./VendorColorSelector');

const VENDOR_COLORS = [
    {
        mark_color_id: 123456,
        color_type: 'NOT_METALLIC',
        hex_codes: [ '4D4D4B' ],
    },
    {
        mark_color_id: 123457,
        color_type: 'NOT_METALLIC',
        hex_codes: [ '4D4D4C' ],
    },
];

const PROMO_MARKER_IMAGE = '//avatars.mds.yandex.net/get-verba/787013/2a00000160935809f855ea7ba87e09e3ea7d/wizardv3mr';

it('должен корректно отрендериться', () => {
    const tree = shallow(
        <VendorColorSelector
            vendorColors={ VENDOR_COLORS }
            promoMarkerImage={ PROMO_MARKER_IMAGE }
            colorIndex={ 0 }
            onSelectMarker={ jest.fn }
        />,
    );
    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('не должен отрендерить ничего, если не передан набор цветов', () => {
    const tree = shallow(
        <VendorColorSelector
            promoMarkerImage={ PROMO_MARKER_IMAGE }
            colorIndex={ 0 }
            onSelectMarker={ jest.fn }
        />,
    );
    expect(shallowToJson(tree)).toHaveLength(0);
});
