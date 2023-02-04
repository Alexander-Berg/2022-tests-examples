const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const DashboardWidgetStatistic = require('www-cabinet/react/components/DashboardWidgetStatistic');

it('должен вернуть корретный компонент', () => {
    expect(shallowToJson(
        shallow(
            <DashboardWidgetStatistic
                className="className"
                compareData={{
                    daily: { last: 20, prev: 20 },
                    weekly: { last: 20, prev: 21 },
                    monthly: { last: 20, prev: 21 },
                }}
            />))).toMatchSnapshot();
});
