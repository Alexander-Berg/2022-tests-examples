const _ = require('lodash');
const React = require('react');
const { shallow } = require('enzyme');

const PlacementProlongationInfoMobile = require('./PlacementProlongationInfoMobile');
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;

let props;
const DEFAULT_PROPS = {
    expireDate: '111',
    placementInfo: {
        auto_prolong_price: 999,
        price: 777,
        days: 7,
    },
    isProlonging: false,
    metrikaFromParam: 'from-lk',
};

beforeEach(() => {
    props = _.cloneDeep(DEFAULT_PROPS);

    contextMock.metrika.sendParams.mockClear();
});

it('при рендере отправит метрику показа', () => {
    shallowRenderComponent(props);

    expect(contextMock.metrika.sendParams).toHaveBeenCalledTimes(1);
    expect(contextMock.metrika.sendParams).toHaveBeenCalledWith([ '7days-placement', 'pop-up', 'shows', 'from-lk' ]);
});

it('при открытии попапа отправит корректную метрику', () => {
    const page = shallowRenderComponent(props);
    const daysLeftBlocl = page.find('.PlacementProlongationInfoMobile__daysLeft');
    daysLeftBlocl.simulate('click');

    expect(contextMock.metrika.sendParams).toHaveBeenCalledTimes(3);
    expect(contextMock.metrika.sendParams.mock.calls[1][0]).toEqual([ '7days-placement', 'pop-up', 'clicks', 'from-lk' ]);
    expect(contextMock.metrika.sendParams.mock.calls[2][0]).toEqual([ '7days-placement', 'landing-page', 'shows', 'from-lk' ]);
});

function shallowRenderComponent(props) {
    const ContextProvider = createContextProvider(contextMock);

    return shallow(
        <ContextProvider>
            <PlacementProlongationInfoMobile { ...props }/>
        </ContextProvider>,
    ).dive();
}
