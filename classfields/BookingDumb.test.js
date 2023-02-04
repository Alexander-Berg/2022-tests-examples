const React = require('react');
const { shallow } = require('enzyme');

const BookingDumb = require('./BookingDumb');

const defaultProps = {
    bookingList: [
        {
            code: 111,
        },
    ],
    onBookingStatusChange: () => {},
    onPageChange: () => {},
    pagination: {
        page_num: 1,
        total_page_count: 1,
    },
};

it('покажет заглушку, если заявок нет', () => {
    const tree = shallow(<BookingDumb bookingList={ [] }/>);

    expect(tree.find('.Booking__placeholder')).toExist();
});

it('покажет список с пагинацией, если заявки есть', () => {
    const tree = shallow(<BookingDumb { ...defaultProps }/>);

    expect(tree.find('.Booking__list')).toExist();
    expect(tree.find('ListingPagination')).toExist();
});
