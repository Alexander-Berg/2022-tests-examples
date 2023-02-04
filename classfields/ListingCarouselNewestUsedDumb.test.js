const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const contextMock = require('autoru-frontend/mocks/contextMock').default;

const ListingCarouselNewestUsedDumb = require('./ListingCarouselNewestUsedDumb');
// eslint-disable-next-line import/no-restricted-paths
const ListingCarouselItem = require('auto-core/react/components/desktop/ListingCarouselItem/ListingCarouselItem').default;

jest.mock('auto-core/react/lib/gateApi', () => ({
    getResource: jest.fn(),
}));

const getResource = require('auto-core/react/lib/gateApi').getResource;

const offerMock = {
    id: 1,
};

const responseMock = {
    offers: Array(6).fill(offerMock),
};

const renderTitle = () => <span>Это заголовок блока</span>;
const renderFooter = () => <span>Это футер блока</span>;

const baseProps = {
    itemComponent: ListingCarouselItem,
    renderTitle,
    renderFooter,
    markName: 'Tesla',
    modelName: 'Model 3',
    searchParameters: {
        catalog_filter: [ { mark: 'TESLA', model: 'MODEL_3' } ],
        section: 'used',
        sort: 'km_age-asc',
        year_from: 2018,
        year_to: 2019,
    },
    listingParameters: {
        catalog_filter: [ { mark: 'TESLA', model: 'MODEL_3' } ],
        section: 'used',
        sort: 'year-desc',
    },
    metrikaParams: 'new,empty,used_cars_block',
};

it('должен отрендерить карусель офферов после получения данных', () => {
    const gateApiPromise = Promise.resolve(responseMock);
    getResource.mockImplementation(() => gateApiPromise);

    const tree = shallow(
        <ListingCarouselNewestUsedDumb
            { ...baseProps }
        />,
        { context: contextMock },
    );

    expect(shallowToJson(tree)).toEqual('');
    return gateApiPromise.then(() => {
        expect(shallowToJson(tree)).toMatchSnapshot();
    });
});

it('не должен отрендерить карусель, если нет офферов в ответе', () => {
    const gateApiPromise = Promise.resolve({
        offers: [],
    });
    getResource.mockImplementation(() => gateApiPromise);

    const tree = shallow(
        <ListingCarouselNewestUsedDumb
            { ...baseProps }

        />,
        { context: contextMock },

    );

    return gateApiPromise.then(() => {
        expect(shallowToJson(tree)).toEqual('');
    });
});
