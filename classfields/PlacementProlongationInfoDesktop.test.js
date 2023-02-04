const _ = require('lodash');
const React = require('react');
const { shallow } = require('enzyme');

const PlacementProlongationInfoDesktop = require('./PlacementProlongationInfoDesktop');
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
    metrikaFromParam: 'from-offer',
};

beforeEach(() => {
    props = _.cloneDeep(DEFAULT_PROPS);
    contextMock.metrika.sendParams.mockClear();
});

it('при рендере отправит метрику показа', () => {
    shallowRenderComponent(props);

    expect(contextMock.metrika.sendParams).toHaveBeenCalledTimes(1);
    expect(contextMock.metrika.sendParams).toHaveBeenCalledWith([ '7days-placement', 'pop-up', 'shows', 'from-offer' ]);
});

it('при открытии попапа отправит корректную метрику', () => {
    const page = shallowRenderComponent(props);
    const popup = page.find('PlacementAutoProlongationModal');
    popup.simulate('showPopup');

    expect(contextMock.metrika.sendParams).toHaveBeenCalledTimes(3);
    expect(contextMock.metrika.sendParams.mock.calls[1][0]).toEqual([ '7days-placement', 'pop-up', 'clicks', 'from-offer' ]);
    expect(contextMock.metrika.sendParams.mock.calls[2][0]).toEqual([ '7days-placement', 'landing-page', 'shows', 'from-offer' ]);
});

function shallowRenderComponent(props) {
    const ContextProvider = createContextProvider(contextMock);

    return shallow(
        <ContextProvider>
            <PlacementProlongationInfoDesktop { ...props }/>
        </ContextProvider>,
    ).dive();
}
