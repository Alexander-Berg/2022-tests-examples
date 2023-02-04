const React = require('react');
const { shallow } = require('enzyme');
const { copyText } = require('auto-core/lib/clipboard');
const _ = require('lodash');

const dealerMock = require('./mock');

const DealerListItem = require('./DealerListItem');

describe('кнопка без телефона', () => {
    it('должен показать номер рыбу телефона в кнопке', function() {
        const tree = shallow(
            <DealerListItem
                key={ dealerMock.dealerId }
                isPhoneShown={ false }
                resultsCountText="100500 авто в продаже"
                { ...dealerMock }
            />,
        );
        const phone = tree.find('ButtonWithLoader').dive().dive().text();

        expect(phone).toEqual('Показать телефон +7 XXX XXX-XX-XX');
    });

    it('должен вызвать функция для похода за телефоном при нажатии', function() {
        const onPhoneClickMock = jest.fn();

        const tree = shallow(
            <DealerListItem
                key={ dealerMock.dealerId }
                isPhoneShown={ false }
                resultsCountText="100500 авто в продаже"
                onPhoneClick={ onPhoneClickMock }
                { ...dealerMock }
            />,
        );
        const newTree = tree.find('ButtonWithLoader');
        newTree.simulate('click');

        expect(onPhoneClickMock).toHaveBeenCalledWith(dealerMock.dealerId, dealerMock.dealerCode, _.noop(), _.noop());
    });
});

jest.mock('auto-core/lib/clipboard', () => ({ copyText: jest.fn() }));
describe('кнопка с телефоном', () => {
    it('должен копировать первый номер при клике на кнопку', () => {
        const tree = shallow(
            <DealerListItem
                key={ dealerMock.dealerId }
                isPhoneShown={ true }
                resultsCountText="100500 авто в продаже"
                { ...dealerMock }
            />,
        );
        tree.find('Button').simulate('click');

        expect(copyText).toHaveBeenCalledWith('+7 800 555-35-35');
    });
});
