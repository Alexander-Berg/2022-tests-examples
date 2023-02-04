const React = require('react');
const { shallow } = require('enzyme');

const RelatedItems = require('./RelatedItems');

it('Должен добавлять бордер с пропом withBorder', () => {
    const wrapper = shallow(
        <RelatedItems
            allLink="Все штуки"
            title="Заголовок для теста"
            url="bar"
            withBorder={ true }
        />);
    expect(wrapper.find('div').at(0).props().className).toEqual(expect.stringContaining('RelatedItems__border'));
});

it('не должен добавлять бордер без withBorder', () => {
    const wrapper = shallow(
        <RelatedItems
            allLink="Все штуки"
            title="Заголовок для теста"
            url="bar"
        />);
    expect(wrapper.find('div').at(0).props().className).toEqual(expect.not.stringContaining('RelatedItems__border'));
});

it('должен добавлять ссылку на все item, если передали url', () => {
    const wrapper = shallow(
        <RelatedItems
            allLink="Все штуки"
            title="Заголовок для теста"
            url="bar"
        />);
    expect(wrapper.exists('Link')).toEqual(true);
});

it('не должен добавлять ссылку на все item, если не передали url', () => {
    const wrapper = shallow(
        <RelatedItems
            allLink="Все штуки"
            title="Заголовок для теста"
        />);
    expect(wrapper.exists('Link')).toEqual(false);
});
