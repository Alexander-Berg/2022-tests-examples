/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

const React = require('react');
const { shallow } = require('enzyme');

const VasGallery = require('./VasGallery');

const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const cloneOfferWithHelpers = require('autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers');
const cardMock = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');
const getIdHash = require('auto-core/react/lib/offer/getIdHash').default;
const configMock = require('auto-core/react/dataDomain/config/mock').default;
const salesMock = require('auto-core/react/dataDomain/sales/mocks').default;
const { getBunkerMock } = require('autoru-frontend/mockData/state/bunker.mock');
const { TOfferVas } = require('auto-core/types/proto/auto/api/api_offer_model');

let initialState;

beforeEach(() => {
    initialState = {
        card: cloneOfferWithHelpers(cardMock).withActiveVas().value(),
        state: {
            activeDialogParams: {
                offerId: getIdHash(cardMock),
                service: 'package_turbo',
                from: 'from-for-billing',
            },
        },
        bunker: getBunkerMock([ 'common/vas_vip' ]),
        config: configMock.withPageType('card').value(),
    };

    contextMock.logVasEvent.mockClear();
});

it('открывается на карточке', () => {
    const page = shallowRenderComponent({ initialState, contextMock });
    expect(page.find('.VasGallery').isEmptyRender()).toBe(false);
});

it('открывается в лк', () => {
    initialState.config = configMock.withPageType('sales').value();
    initialState.sales = salesMock.value();
    const page = shallowRenderComponent({ initialState, contextMock });
    expect(page.find('.VasGallery').isEmptyRender()).toBe(false);
});

describe('при смене слайда', () => {
    it('для неподключенного сервиса отправит вас лог', () => {
        const page = shallowRenderComponent({ initialState, contextMock });
        const gallery = page.find('ReactImageGallery');
        gallery.simulate('slide', 2);

        expect(contextMock.logVasEvent).toHaveBeenCalledTimes(1);
        expect(contextMock.logVasEvent.mock.calls[0][0]).toMatchSnapshot();
    });

    it('для подключенного сервиса ничего не будет делать', () => {
        initialState.card = cloneOfferWithHelpers(cardMock).withActiveVas([ TOfferVas.STORIES ]).value();
        const page = shallowRenderComponent({ initialState, contextMock });
        const gallery = page.find('ReactImageGallery');
        gallery.simulate('slide', 2);

        expect(contextMock.logVasEvent).toHaveBeenCalledTimes(0);
    });
});

describe('при свайпе', () => {
    it('если свайп больше 50% экрана, поменяет класс VasGallery_backdrop_type у контейнера галереи', () => {
        const page = shallowRenderComponent({ initialState, contextMock });
        const gallery = page.find('ReactImageGallery');
        gallery.simulate('swipe', 51, -1, 1);

        const galleryContainer = page.find('.VasGallery');
        expect(galleryContainer.hasClass('VasGallery_backdrop_type_show-in-stories')).toBe(true);
        expect(galleryContainer.hasClass('VasGallery_backdrop_type_package_turbo')).toBe(false);
    });

    it('если свайп 50% и меньше экрана, не поменяет класс VasGallery_backdrop_type у контейнера галереи', () => {
        const page = shallowRenderComponent({ initialState, contextMock });
        const gallery = page.find('ReactImageGallery');
        gallery.simulate('swipe', 50, -1, 1);

        const galleryContainer = page.find('.VasGallery');
        expect(galleryContainer.hasClass('VasGallery_backdrop_type_show-in-stories')).toBe(false);
        expect(galleryContainer.hasClass('VasGallery_backdrop_type_package_turbo')).toBe(true);
    });

    it('корректно отработает при свайпе от первого слайда к последнему', () => {
        const page = shallowRenderComponent({ initialState, contextMock });
        const gallery = page.find('ReactImageGallery');
        gallery.simulate('swipe', 51, 1, 0);

        const galleryContainer = page.find('.VasGallery');
        expect(galleryContainer.hasClass('VasGallery_backdrop_type_all_sale_fresh')).toBe(true);
    });

    it('корректно отработает при свайпе от последнего слайда к первому', () => {
        const page = shallowRenderComponent({ initialState, contextMock });
        const gallery = page.find('ReactImageGallery');
        gallery.simulate('swipe', 51, -1, 6);

        const galleryContainer = page.find('.VasGallery');
        expect(galleryContainer.hasClass('VasGallery_backdrop_type_package_vip')).toBe(true);
    });
});

describe('список сервисов', () => {
    it('будет включать "вип" и "сторисы", если вип активен', () => {
        initialState.card = cloneOfferWithHelpers(cardMock).withActiveVas([ TOfferVas.VIP ]).value();
        initialState.state.activeDialogParams.service = TOfferVas.VIP;

        const page = shallowRenderComponent({ initialState, contextMock });
        const galleryItems = page.find('ReactImageGallery').prop('items').map(({ service }) => service);

        expect(galleryItems).toHaveLength(2);
        expect(galleryItems[0]).toBe(TOfferVas.VIP);
        expect(galleryItems[1]).toBe(TOfferVas.STORIES);
    });

    it('если для объявления доступен вип, будет включать правильный набор сервисов в правильном порядке', () => {
        const page = shallowRenderComponent({ initialState, contextMock });
        const galleryItems = page.find('ReactImageGallery').prop('items').map(({ service }) => service);

        expect(galleryItems).toMatchSnapshot();
    });

    it('если для объявления не доступен вип, будет включать правильный набор сервисов в правильном порядке', () => {
        initialState.card = cloneOfferWithHelpers(cardMock)
            .withCustomVas({ service: 'package_vip', recommendation_priority: 0 })
            .withPrice(100000)
            .value();

        const page = shallowRenderComponent({ initialState, contextMock });
        const galleryItems = page.find('ReactImageGallery').prop('items').map(({ service }) => service);

        expect(galleryItems).toMatchSnapshot();
    });
});

function shallowRenderComponent({ contextMock, initialState }) {
    const ContextProvider = createContextProvider(contextMock);
    const store = mockStore(initialState);

    return shallow(
        <ContextProvider>
            <VasGallery { ...initialState } store={ store }/>
        </ContextProvider>,
    ).dive().dive();
}
