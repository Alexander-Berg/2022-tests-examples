const React = require('react');

const SaleAutostrategy = require('./SaleAutostrategy');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const _ = require('lodash');
import contextMock from 'autoru-frontend/mocks/contextMock';

it('render тест: должен вернуть корректный компонент', () => {
    expect(shallowToJson(shallow(<SaleAutostrategy
        autostrategy={{ id: 'autostrategyId' }}
        clientCityId={ 123 }
        clientRegionId={ 222 }
        isServicesDisabled={ true }
        offerID="111-222"
        canWriteSaleResource={ true }
    />, { context: contextMock }))).toMatchSnapshot();
});

it('onAutostrategyClick тест: должен вызвать openAutostrategy c корректными параметрами', () => {
    const openAutostrategy = jest.fn();
    const sendPageEvent = jest.fn();
    const reachGoal = jest.fn();
    const saleAutostrategy = shallow(<SaleAutostrategy
        clientCityId={ 111 }
        clientRegionId={ 222 }
        openAutostrategy={ openAutostrategy }
    />, {
        context: {
            metrika: {
                sendPageEvent,
                reachGoal,
                params: _.noop,
                sendPageAuthEvent: _.noop,
            },
        },
    });
    saleAutostrategy.instance().onClick();
    expect(sendPageEvent).toHaveBeenCalledWith([ 'autostrategy', 'show_popup' ]);
    expect(reachGoal).toHaveBeenCalledWith('AUTOSTRATEGY_BUTTON_CLICK');
    expect(openAutostrategy).toHaveBeenCalledWith({
        activated: false,
        regionId: 222,
        salonGeoId: 111,
    });
});
