jest.mock('auto-core/lib/rum/onChangeRumPage', () => jest.fn());
jest.mock('auto-core/router/cabinet.auto.ru/react/susanin', () => {
    return {
        getRouteByName: jest.fn(),
    };
});

const onChangeRumPage = require('auto-core/lib/rum/onChangeRumPage');
const susanin = require('auto-core/router/cabinet.auto.ru/react/susanin');

const React = require('react');
const { shallow } = require('enzyme');

const RumPageUpdaterDumb = require('./RumPageUpdaterDumb');

it('Должен отрендерить попап со всеми элементами', () => {
    susanin.getRouteByName.mockImplementation((routeName) => {
        if (routeName === 'foo') {
            return { getData: () => ({ controller: 'foo_controller' }) };
        }

        if (routeName === 'bar') {
            return { getData: () => ({ controller: 'bar_controller' }) };
        }
    });

    const pageUpdater = shallow(
        <RumPageUpdaterDumb
            routeName="foo"
        />,
    );

    pageUpdater.setProps({ routeName: 'bar' });

    expect(onChangeRumPage).toHaveBeenCalledWith('jest.bar_controller');
});
