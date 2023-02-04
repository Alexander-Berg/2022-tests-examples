/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const { Provider } = require('react-redux');

const mockStore = require('autoru-frontend/mocks/mockStore').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const configStateMock = require('auto-core/react/dataDomain/config/mock').default;
const listingMock = require('autoru-frontend/mockData/state/listingForGroup.mock');

const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const Context = createContextProvider(contextMock);

const ListingCarousel = require('./ListingCarousel');

const storeDesktop = mockStore({
    listing: listingMock,
    config: configStateMock.value(),
});

const storeMobile = mockStore({
    listing: listingMock,
    config: {
        data: {
            pageParams: {
                deviceType: 'mobile',
            },
        },
    },
});

it('Должен построить компонент для desktop и 6 картинок в карусели на оффер', () => {
    const tree = shallowRenderComponent(storeDesktop);
    expect(shallowToJson(tree).children[0].children[0].props.offer.state.image_urls).toHaveLength(6);
});

it('Должен построить компонент для touch и 1 картинку в карусели на оффер', () => {
    const tree = shallowRenderComponent(storeMobile);
    expect(shallowToJson(tree).children[0].children[0].props.offer.state.image_urls).toHaveLength(1);
});

describe('ссылки', () => {
    const wrapper = shallowRenderComponent(storeMobile);

    it('правильно формирует ссылку для заголовка', () => {
        expect(
            wrapper.find('CarouselUniversal').prop('pageLink'),
        ).toBe('link/index/?utm_source=yandex-zen&utm_campaign=widget-listing-carousel&id=&from=autoru-widget-carousel-listing');
    });

    it('правильно формирует ссылку для кнопки "Больше объявлений"', () => {
        expect(
            wrapper.find('CarouselUniversal').dive().find('Link.ListingCarousel__moreInfoLink').prop('url'),
        ).toBe('link/listing/?utm_source=yandex-zen&utm_campaign=widget-listing-carousel&id=&from=autoru-widget-carousel-listing&deviceType=mobile');
    });

    it('правильно формирует utm метки ссылки для элементов карусели', () => {
        expect(
            wrapper.find('CarouselUniversal').dive().find('ListingItemCarousel').at(0).prop('utmParams'),
        ).toEqual({ utm_campaign: 'widget-listing-carousel', utm_source: 'yandex-zen' });
    });
});

function shallowRenderComponent(store) {
    const wrapper = shallow(
        <Context>
            <Provider store={ store }>
                <ListingCarousel/>
            </Provider>
        </Context>,
    );

    return wrapper.dive().dive().dive();
}
