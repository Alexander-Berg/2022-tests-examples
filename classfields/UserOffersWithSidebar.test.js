const React = require('react');
const { shallow } = require('enzyme');

const UserOffersWithSidebar = require('./UserOffersWithSidebar');

const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const withAuthUserMock = require('auto-core/react/dataDomain/user/mocks/withAuth.mock');
const moderationStatusStateMock = require('autoru-frontend/mockData/state/moderationStatus.mock');
const { getBunkerMock } = require('autoru-frontend/mockData/state/bunker.mock');
const salesMock = require('auto-core/react/dataDomain/sales/mocks').default;
const cloneOfferWithHelpers = require('autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers');
const cardStateMock = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');
const getIdHash = require('auto-core/react/lib/offer/getIdHash').default;

let initialState;
let props;

beforeEach(() => {
    props = {
        params: {
            category: 'cars',
        },
    };
    initialState = {
        bunker: getBunkerMock([ 'desktop/support-contacts' ]),
        user: withAuthUserMock,
        sales: salesMock.value(),
        drafts: {
            data: [],
        },
        c2bApplications: {
            applications: [],
        },
        moderationStatus: moderationStatusStateMock,
    };
});

const commonProps = {
    params: {},
};

describe('баннер "помощь рядом"', () => {
    it('покажется, если есть один неактивный оффер', () => {
        initialState.sales.items = [
            cloneOfferWithHelpers(cardStateMock).withStatus('INACTIVE').value(),
        ];
        const page = shallowRenderComponent({ initialState, props });
        const yandexHelpBanner = page.find('BannerYandexHelp');

        expect(yandexHelpBanner.isEmptyRender()).toBe(false);
    });

    it('не покажется, если есть один активный оффер', () => {
        initialState.sales.items = [
            cloneOfferWithHelpers(cardStateMock).withStatus('ACTIVE').value(),
        ];
        const page = shallowRenderComponent({ initialState, props });
        const yandexHelpBanner = page.find('BannerYandexHelp');

        expect(yandexHelpBanner.isEmptyRender()).toBe(true);
    });

    it('не покажется, если есть два и более офферов', () => {
        initialState.sales.items = [
            cloneOfferWithHelpers(cardStateMock).withStatus('INACTIVE').value(),
            cloneOfferWithHelpers(cardStateMock).withStatus('ACTIVE').value(),
        ];
        const page = shallowRenderComponent({ initialState, props });
        const yandexHelpBanner = page.find('BannerYandexHelp');

        expect(yandexHelpBanner.isEmptyRender()).toBe(true);
    });
});

describe('фильтры', () => {
    it('не покажет, если кол-во офферов меньше пяти', () => {
        const page = shallowRenderComponent({ initialState, props });
        const filters = page.find('Connect(SalesFilters)');

        expect(filters.isEmptyRender()).toBe(true);
    });

    it('покажет, если кол-во офферов больше пяти', () => {
        initialState.sales = salesMock.withNumOffers(6).value();
        const page = shallowRenderComponent({ initialState, props });
        const filters = page.find('Connect(SalesFilters)');

        expect(filters.isEmptyRender()).toBe(false);
    });

    it('покажет, если в сторе есть параметры поиска', () => {
        initialState.sales = salesMock.withSearchParams({ price_from: 7000000 }).value();
        const page = shallowRenderComponent({ initialState, props });
        const filters = page.find('Connect(SalesFilters)');

        expect(filters.isEmptyRender()).toBe(false);
    });

    it('если при фильтрации офферы не найдены, покажет заглушку', () => {
        initialState.sales = salesMock.withSearchParams({ price_from: 7000000 }).withNumOffers(0).value();
        const page = shallowRenderComponent({ initialState, props });
        const contentPlaceholder = page.find('.UserOffersWithSidebar__noOffersFound');

        expect(contentPlaceholder.isEmptyRender()).toBe(false);
    });
});

describe('черновик', () => {
    it('покажет черновик на последней странице', () => {
        initialState.sales = salesMock.withNumOffers(1).withPagination({ total_page_count: 0 }).value();
        initialState.drafts.data = [
            { offer: cloneOfferWithHelpers(cardStateMock).withStatus('DRAFT').withSaleId('aaa-111').value(), offer_id: 'aaa-111' },
        ];
        const page = shallowRenderComponent({ initialState, props });
        const items = page.find('ConnectSocialAccountHoc(Connect(SalesItem))');
        const drafts = items.findWhere((element) => getIdHash(element.prop('offer')) === 'aaa-111');

        expect(items).toHaveLength(2);
        expect(drafts).toHaveLength(1);
    });

    it('не покажет черновик если страница не последняя', () => {
        initialState.sales = salesMock.withNumOffers(1).withPagination({ total_page_count: 2 }).value();
        initialState.drafts.data = [
            { offer: cloneOfferWithHelpers(cardStateMock).withStatus('DRAFT').withSaleId('aaa-111').value(), offer_id: 'aaa-111' },
        ];
        const page = shallowRenderComponent({ initialState, props });
        const items = page.find('ConnectSocialAccountHoc(Connect(SalesItem))');
        const drafts = items.findWhere((element) => getIdHash(element.prop('offer')) === 'aaa-111');

        expect(drafts).toHaveLength(0);
    });

    it('не покажет черновик если применены фильтры', () => {
        initialState.sales = salesMock.withNumOffers(0).withPagination({ total_page_count: 0 }).withSearchParams({ price_from: 7000000 }).value();
        initialState.drafts.data = [
            { offer: cloneOfferWithHelpers(cardStateMock).withStatus('DRAFT').withSaleId('aaa-111').value(), offer_id: 'aaa-111' },
        ];
        const page = shallowRenderComponent({ initialState, props });
        const items = page.find('ConnectSocialAccountHoc(Connect(SalesItem))');
        const drafts = items.findWhere((element) => getIdHash(element.prop('offer')) === 'aaa-111');

        expect(drafts).toHaveLength(0);
    });
});

function shallowRenderComponent({ initialState, props }) {
    const ContextProvider = createContextProvider(contextMock);
    const store = mockStore(initialState);

    return shallow(
        <ContextProvider>
            <UserOffersWithSidebar { ...commonProps } { ...props } store={ store }/>
        </ContextProvider>,
    ).dive().dive();
}
