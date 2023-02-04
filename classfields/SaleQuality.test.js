jest.mock('www-cabinet/react/lib/sale/getQuality');
jest.mock('www-cabinet/react/lib/sale/getQualityColor', () => () => 'color');

const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const SaleQuality = require('./SaleQuality');

it('должен вернуть корректный элемент, если recommendations.length = 0', () => {
    const getQuality = require('www-cabinet/react/lib/sale/getQuality');
    getQuality.mockImplementation(() => ({
        recommendations: [],
        value: 10,
    }));
    const saleQuality = shallow(<SaleQuality
        images={ [ 1, 2, 3 ] }
        overprice=""
        vin="123"
    />);

    expect(shallowToJson(saleQuality)).toMatchSnapshot();
});

it('должен вернуть корректный элемент, если recommendations.length > 0', () => {
    const getQuality = require('www-cabinet/react/lib/sale/getQuality');
    getQuality.mockImplementation(() => ({
        recommendations: [
            { text: 'text1', profit: 10 },
            { text: 'text2', profit: 20 },
        ],
        value: 20,
    }));

    const saleQuality = shallow(<SaleQuality
        images={ [ 1, 2, 3 ] }
        overprice=""
        vin="123"
    />);

    expect(shallowToJson(saleQuality)).toMatchSnapshot();
});
