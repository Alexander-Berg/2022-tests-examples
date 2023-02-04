const React = require('react');
const { shallow } = require('enzyme');
const _ = require('lodash');
const BackOnSaleItem = require('./BackOnSaleItem');
const backOnSaleProps = require('./mocks/backOnSaleProps');
const contextMock = require('autoru-frontend/mocks/contextMock').default;

it('должен вернуть показать бейдж "продан", если isSold', () => {
    const props = _.cloneDeep(backOnSaleProps);
    props.offer.status = 'INACTIVE';
    const comeback = shallow(<BackOnSaleItem { ...props }/>, { context: contextMock });

    expect(comeback.find('.BackOnSaleItem__soldBadge')).toExist();
});

it('renderInfo: должен вернуть информацию об объявлении', () => {
    const comeback = shallow(<BackOnSaleItem { ...backOnSaleProps }/>, { context: contextMock });
    expect(comeback.instance().renderInfo()).toMatchSnapshot();
});

describe('renderPhones', () => {
    it('должен нарисовать кнопку показа телефона', () => {
        const props = _.cloneDeep(backOnSaleProps);
        props.offer.seller = {};
        const comeback = shallow(<BackOnSaleItem { ...props }/>, { context: contextMock });
        expect(comeback.instance().renderPhones()).toMatchSnapshot();
    });

    it('должен нарисовать список с номерами телефонов, если они есть у seller', () => {
        const comeback = shallow(<BackOnSaleItem { ...backOnSaleProps }/>, { context: contextMock });
        expect(comeback.instance().renderPhones()).toMatchSnapshot();
    });
});

it('должен нарисовать кнопку Написать', () => {
    const comeback = shallow(<BackOnSaleItem { ...backOnSaleProps }/>, { context: contextMock });
    expect(comeback.instance().renderChatButton()).toMatchSnapshot();
});
