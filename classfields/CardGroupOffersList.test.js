const _ = require('lodash');
const React = require('react');
const { shallow } = require('enzyme');

const cardMock = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;

const CardGroupOffersList = require('./CardGroupOffersList');

let offers;

beforeEach(() => {
    offers = [ cardMock, cardMock, cardMock, cardMock ];
});

describe('Блок CardGroupBestPrice', () => {
    describe('рендеринг', () => {
        it('рендерит если есть shouldDisplayMatchApplication', () => {
            const wrapper = shallowRenderComponent({ offers, context: contextMock, shouldDisplayMatchApplication: true });

            const component = wrapper.find('Connect(CardGroupBestPrice)');
            expect(component).toExist();
        });

        it('не рендерит, если есть shouldDisplayMatchApplication, но страница не первая', () => {
            const wrapper = shallowRenderComponent({ offers, context: contextMock, shouldDisplayMatchApplication: true, pageParams: { page: 2 } });

            const component = wrapper.find('Connect(CardGroupBestPrice)');
            expect(component).not.toExist();
        });
    });

    describe('положение блока', () => {
        it('должен вставить провязку после 3 оффера', () => {
            const wrapper = shallowRenderComponent({ offers, context: contextMock, shouldDisplayMatchApplication: true });

            const offersList = wrapper.find('.CardGroupOffersList__items');

            expect(offersList.childAt(3).is('Connect(CardGroupBestPrice)')).toBe(true);
        });

        it('должен вставить провязку последней, если меньше 3 офферов', () => {
            const wrapper = shallowRenderComponent({ offers: [ cardMock ], context: contextMock, shouldDisplayMatchApplication: true });

            const offersList = wrapper.find('.CardGroupOffersList__items');

            expect(offersList.childAt(1).is('Connect(CardGroupBestPrice)')).toBe(true);
        });
    });
});

const shallowRenderComponent = ({ offers, context, shouldDisplayMatchApplication, pageParams = {} }) => {
    const Context = createContextProvider(context);

    return shallow(
        <Context>
            <CardGroupOffersList
                offers={ offers }
                pageParams={ pageParams }
                shouldDisplayMatchApplication={ shouldDisplayMatchApplication }
                equipmentDictionary={{}}
                presentEquipment={ [] }
                sendMarketingEventByListingOffer={ _.noop }
                sellerPopupOpen={ _.noop }
            />
        </Context>,
    ).dive();
};
