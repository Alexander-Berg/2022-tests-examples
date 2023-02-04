const React = require('react');

const { shallow } = require('enzyme');

const VinReportHistoryOffer = require('./VinReportHistoryOffer');

it('должен отрисовать дату и место, если они есть', () => {
    const wrapper = shallow(
        <VinReportHistoryOffer
            record={{
                title: 'title',
                time_of_placement: '1526355600000',
                time_of_removal: '1556355600000',
                mileage: 24000,
                region_name: 'Москва',
                offer_link: 'url',
            }}/>,
    );

    expect(wrapper.find('VinReportHistoryRecord').prop('date')).toEqual('15 мая 2018, Москва');
});

it('должен отрисовать только дату, если нет места', () => {
    const wrapper = shallow(
        <VinReportHistoryOffer
            record={{
                title: 'title',
                time_of_placement: '1526355600000',
                time_of_removal: '1556355600000',
                mileage: 24000,
                offer_link: 'url',
            }}/>,
    );

    expect(wrapper.find('VinReportHistoryRecord').prop('date')).toEqual('15 мая 2018');
});
