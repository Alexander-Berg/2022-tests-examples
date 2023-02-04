const React = require('react');
const { shallow } = require('enzyme');
const BookingItem = require('./BookingItem');
const _ = require('lodash');
const contextMock = require('autoru-frontend/mocks/contextMock').default;

it('должен нарисовать кнопки, если пользователю разрешен доступ на запись', () => {
    const booking = {
        status: 'PAID',
        user: {},
        offer: {
            category: 'cars',
        },
    };
    const bookingItem = shallow(
        <BookingItem
            canWriteBookingResource={ true }
            booking={ booking }
            onStatusChange={ _.noop }
        />, { context: contextMock },
    );

    expect(bookingItem.find('.BookingItem__actionButton')).toHaveLength(4);
});

it('не должен рендерить кнопки, если пользователю запрешен доступ на запись', () => {
    const booking = {
        status: 'PAID',
        user: {},
        offer: {
            category: 'cars',
        },
    };
    const bookingItem = shallow(
        <BookingItem
            canWriteBookingResource={ false }
            booking={ booking }
            onStatusChange={ _.noop }
        />, { context: contextMock },
    );
    expect(bookingItem.find('.BookingItem__actionButton')).toHaveLength(0);
});
