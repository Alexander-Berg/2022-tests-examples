/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/lib/event-log/statApi');

const { noop } = require('lodash');
const React = require('react');
const { shallow } = require('enzyme');

const configStateMock = require('auto-core/react/dataDomain/config/mock').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const cardMock = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');
const cloneOfferWithHelpers = require('autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers');
const vinReportMock = require('auto-core/react/dataDomain/defaultVinReport/mocks/defaultVinReport.mock').data;
const userWithAuthMock = require('auto-core/react/dataDomain/user/mocks/withAuth.mock');
const { getBunkerMock } = require('autoru-frontend/mockData/state/bunker.mock');

const statApi = require('auto-core/lib/event-log/statApi').default;

const CardImageGallerySidebar = require('./CardImageGallerySidebar');

let store;
beforeEach(() => {
    store = mockStore({
        bunker: getBunkerMock([ 'common/metrics' ]),
        config: configStateMock.value(),
        user: { data: {} },
        matchApplication: { allowedMarksModels: {}, markModelDetails: [ {} ] },
    });
});

it('отправит событие chat_init_event во фронтлог', () => {
    window.vertis_chat = { open_chat_for_offer: jest.fn() };
    contextMock.hasExperiment.mockImplementationOnce(() => true);

    const offer = cloneOfferWithHelpers(cardMock).withSection('new').withChatOnly(true).withIsOwner(false).value();
    const Context = createContextProvider(contextMock);

    const page = shallow(
        <Context>
            <CardImageGallerySidebar
                offer={ offer }
                sendEventsToMarketing={ noop }
                vinReport={ vinReportMock }
                store={ store }
                hasStateSupport={ true }
            />
        </Context>,
        {
            context: {
                ...contextMock,
                store: mockStore({
                    bunker: getBunkerMock([ 'common/metrics' ]),
                    config: configStateMock.withPageType('card').value(),
                    user: userWithAuthMock,
                    matchApplication: { allowedMarksModels: {}, markModelDetails: [ {} ] },
                    card: offer,
                }),
            },
        },
    ).dive().dive();

    page.find('Connect(OpenChatByOffer)').first().dive().dive().simulate('click');

    expect(statApi.logImmediately).toHaveBeenCalledTimes(1);
    expect(statApi.logImmediately).toHaveBeenCalledWith({
        chat_init_event: {
            card_from: 'SERP',
            card_id: '1085562758-1970f439',
            category: 'CARS',
            context_block: 'BLOCK_GALLERY',
            context_page: 'PAGE_CARD',
            search_query_id: '',
            section: 'USED',
            self_type: 'TYPE_SINGLE',
            trade_in_allowed: false,
        },
    });
});

describe('программа господдержки', () => {
    it('покажет скидку, если оффер входит в программу', () => {
        const offer = cloneOfferWithHelpers(cardMock).withSection('new').withChatOnly(true).withIsOwner(false).value();
        const Context = createContextProvider(contextMock);

        const page = shallow(
            <Context>
                <CardImageGallerySidebar
                    offer={ offer }
                    sendEventsToMarketing={ noop }
                    vinReport={ vinReportMock }
                    store={ store }
                    hasStateSupport={ true }
                />
            </Context>,
        ).dive().dive();

        const discountList = page.find('CardDiscountList');

        expect(discountList.prop('hasStateSupport')).toBe(true);
    });

    it('не покажет скидку, если оффер не входит в программу', () => {
        const offer = cloneOfferWithHelpers(cardMock).withSection('new').withChatOnly(true).withIsOwner(false).value();
        const Context = createContextProvider(contextMock);

        const page = shallow(
            <Context>
                <CardImageGallerySidebar
                    offer={ offer }
                    sendEventsToMarketing={ noop }
                    vinReport={ vinReportMock }
                    store={ store }
                    hasStateSupport={ false }
                />
            </Context>,
        ).dive().dive();

        const discountList = page.find('CardDiscountList');

        expect(discountList.prop('hasStateSupport')).toBe(false);
    });
});

describe('Блок Показать лучшую цену', () => {
    it('показать блок когда если передается проп shouldShowBestPriceBlock', () => {
        const Context = createContextProvider(contextMock);

        const page = shallow(
            <Context>
                <CardImageGallerySidebar
                    offer={ cardMock }
                    sendEventsToMarketing={ noop }
                    store={ store }
                    shouldShowBestPriceBlock
                />
            </Context>,
        ).dive().dive();

        const bestPriceBlock = page.find('GalleryBestPriceSidebar');

        expect(bestPriceBlock.exists()).toBe(true);
    });

    it('не показывать блок когда если проп shouldShowBestPriceBlock не передается', () => {
        const Context = createContextProvider(contextMock);
        const offer = cloneOfferWithHelpers(cardMock).withMatchApplicationContexts([ 'gallery' ]).value();

        const page = shallow(
            <Context>
                <CardImageGallerySidebar
                    offer={ offer }
                    sendEventsToMarketing={ noop }
                    store={ store }
                />
            </Context>,
        ).dive().dive();

        const bestPriceBlock = page.find('GalleryBestPriceSidebar');

        expect(bestPriceBlock.exists()).toBe(false);
    });
});
