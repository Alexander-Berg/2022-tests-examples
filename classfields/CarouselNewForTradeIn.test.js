const React = require('react');
const { shallow } = require('enzyme');
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;

const contextMock = require('autoru-frontend/mocks/contextMock').default;

const Context = createContextProvider(contextMock);

const CarouselNewForTradein = require('./CarouselNewForTradeIn');

jest.mock('auto-core/react/lib/gateApi', () => ({
    getResource: jest.fn(),
}));

const getResource = require('auto-core/react/lib/gateApi').getResource;

const PROPS = {
    mark: 'AUDI',
    model: 'A4',
    year: '2017',
    tradeInEstimatedCost: 400000,

};

function delayedExpect(func, done, timeout = 100) {
    setTimeout(() => {
        try {
            func();
            done();
        } catch (e) {
            done(e);
        }
    }, timeout);
}

it('должен дернуть ручку новых для трейдина и передать туда марку, модель и год', () => {
    return new Promise((done) => {
        getResource.mockImplementation(() => Promise.resolve());
        const tree = shallow(
            <Context>
                <CarouselNewForTradein { ...PROPS }/>
            </Context >,
        ).dive().find('CarouselLazyOffers').dive().dive();
        const instance = tree.instance();
        instance.handleIntersectionChange(true);
        delayedExpect(
            () => {
                expect(getResource).toHaveBeenCalledWith('getNewOffersForTradeIn', {
                    mark: 'AUDI',
                    model: 'A4',
                    year: '2017',
                    tradeInEstimatedCost: 400000,
                });
            },
            done,
        );
    });
});
