const React = require('react');
const { Provider } = require('react-redux');

const { shallow } = require('enzyme');
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const DateMock = require('autoru-frontend/mocks/components/DateMock');
const cloneOfferWithHelpers = require('autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers');

const cardMock = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');

const CardHead = require('./CardHead');

const store = mockStore({
    ads: {},
    banks: {},
    credit: {},
    cookies: {},
    listing: {},
    favorites: {},
    compare: {},
    bunker: {},
    config: {},
    tradein: {},
    user: {},
});

const offer = cloneOfferWithHelpers(cardMock)
    .withIsOwner(true)
    .withBaseDate('2019-08-16')
    .value();

describe('VasBlock', () => {
    it('рендерит блок без экспа', () => {
        const wrapper = shallowRenderWrapper();
        expect(wrapper.find('Connect(CardVAS)')).toExist();
    });

    it('не рендерит блок в экспе', () => {
        const hasExperiment = exp => exp === 'AUTORUFRONT-19219_new_lk_and_vas_block_design';
        const wrapper = shallowRenderWrapper(hasExperiment);
        expect(wrapper.find('Connect(CardVAS)')).not.toExist();
    });
});

function shallowRenderWrapper(hasExperiment) {
    const Context = createContextProvider({
        ...contextMock,
        ...hasExperiment && { hasExperiment },
    });

    return shallow(
        <DateMock date="2019-08-16">
            <Context>
                <Provider store={ store }>
                    <CardHead
                        offer={ offer }
                        isCardNew={ false }
                    />
                </Provider>
            </Context >
        </DateMock>,
    ).dive().dive().dive();
}
