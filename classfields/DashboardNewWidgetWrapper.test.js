jest.mock('auto-core/react/lib/cookie', () => {
    return {
        get: jest.fn(),
        setForever: jest.fn(),
    };
});

const React = require('react');
const { shallow } = require('enzyme');

const cookie = require('auto-core/react/lib/cookie');

const DashboardNewWidgetWrapper = require('./DashboardNewWidgetWrapper');

jest.useFakeTimers();

it('должен обновлять стейт и сеттить куку, если нет куки', () => {
    cookie.get.mockImplementation(() => false);

    const tree = shallow(
        <DashboardNewWidgetWrapper cookie="test"/>,
    );

    jest.runAllTimers();

    const instance = tree.instance();

    expect(instance.state.isVisibleOverlay).toBe(true);
    expect(cookie.setForever).toHaveBeenCalledWith('test', '1');
});

it('не должен ничего делать, если есть кука', () => {
    cookie.get.mockImplementation(() => true);

    const tree = shallow(
        <DashboardNewWidgetWrapper cookie="test"/>,
    );

    jest.runAllTimers();

    const instance = tree.instance();

    expect(instance.state.isVisibleOverlay).toBe(false);
    expect(cookie.setForever).not.toHaveBeenCalled();
});
