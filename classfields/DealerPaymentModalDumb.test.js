const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const DealerPaymentModalDumb = require('./DealerPaymentModalDumb');

describe('render тесты', () => {
    it('должен вернуть кооректный элемент', () => {
        expect(shallowToJson(shallow(
            <DealerPaymentModalDumb
                price={ 150 }
                onCloseClick={ jest.fn() }
                onConfirmClick={ jest.fn() }
            />))).toMatchSnapshot();
    });

    it('должен вернуть кооректный элемент, если isMobile = true', () => {
        expect(shallowToJson(shallow(
            <DealerPaymentModalDumb
                isMobile
                isOpened
                price={ 150 }
                onCloseClick={ jest.fn() }
                onConfirmClick={ jest.fn() }
            />))).toMatchSnapshot();
    });
});

it('onCloseButtonClick тест: должен вызвать onClick и sendPageEvent с корректными параметрами', () => {
    const sendPageEvent = jest.fn();
    const onCloseClick = jest.fn();
    const dealerPaymentModalDumb = shallow(
        <DealerPaymentModalDumb
            price={ 150 }
            onCloseClick={ onCloseClick }
            onConfirmClick={ jest.fn() }
        />, {
            context: {
                metrika: {
                    sendPageEvent,
                    reachGoal: () => {},
                    params: () => {},
                    sendPageAuthEvent: () => {},
                },
            },
        });

    dealerPaymentModalDumb.instance().onCloseButtonClick();

    expect(onCloseClick).toHaveBeenCalledWith();
    expect(sendPageEvent).toHaveBeenCalledWith([ 'history_report', 'payment', 'dealer_cancel' ]);
});

it('onConfirmButtonClick тест: должен вызвать onClick и sendPageEvent с корректными параметрами', () => {
    const sendPageEvent = jest.fn();
    const onConfirmClick = jest.fn();
    const dealerPaymentModalDumb = shallow(
        <DealerPaymentModalDumb
            price={ 150 }
            onCloseClick={ jest.fn() }
            onConfirmClick={ onConfirmClick }
        />, {
            context: {
                metrika: {
                    sendPageEvent,
                    reachGoal: () => {},
                    params: () => {},
                    sendPageAuthEvent: () => {},
                },
            },
        });

    dealerPaymentModalDumb.instance().onConfirmButtonClick();

    expect(onConfirmClick).toHaveBeenCalledWith();
    expect(sendPageEvent).toHaveBeenCalledWith([ 'history_report', 'payment', 'dealer_buy' ]);
});
