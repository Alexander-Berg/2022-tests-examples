/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

const React = require('react');
const { shallow } = require('enzyme');
const DeliverySettingsSuggest = require('./DeliverySettingsSuggest');
jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(),
    };
});

it('renderSuggestItemContent тест: должен вернуть корректный компонент', () => {
    const DeliverySettingsSuggestInstance = shallow(
        <DeliverySettingsSuggest/>).instance();

    expect(DeliverySettingsSuggestInstance.renderSuggestItemContent({ value: 'address' })).toMatchSnapshot();
});

it('getSuggestData тест: должен вызвать getGeoSuggest и вернуть корректный объект, если category === trucks', () => {
    const getGeoSuggest = jest.fn(() => Promise.resolve({
        response: [
            '111',
            [
                {
                    lat: 1,
                    lon: 1,
                    name: 'name 1',
                    kind: 'kind 1',
                },
                {
                    lat: 2,
                    lon: 2,
                    name: 'name 2',
                    kind: 'kind 2',
                },
                {
                    lat: 3,
                    lon: 3,
                    name: 'name 3',
                    kind: 'kind 3',
                },
            ],
        ],
    }));
    const DeliverySettingsSuggestInstance = shallow(
        <DeliverySettingsSuggest
            getGeoSuggest={ getGeoSuggest }
            addressNeeded={ false }
        />).instance();

    return DeliverySettingsSuggestInstance.getSuggestData('address').then(result => {
        expect(getGeoSuggest).toHaveBeenCalledWith({
            results: 5,
            'in': 225,
            reverse_geo_name: 0,
            text: 'address',
            v: 8,
            search_type: 'addr',
        });
        expect(result).toEqual([
            {
                latitude: 1,
                longitude: 1,
                value: 'name 1',
                kind: 'kind 1',
            },
            {
                latitude: 2,
                longitude: 2,
                value: 'name 2',
                kind: 'kind 2',
            },
            {
                latitude: 3,
                longitude: 3,
                value: 'name 3',
                kind: 'kind 3',
            },
        ]);
    });
});
