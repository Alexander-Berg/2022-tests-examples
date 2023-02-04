/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

const React = require('react');
const { shallow } = require('enzyme');

const PanoramaEmbedded = require('./PanoramaEmbedded');

const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const cardStateMock = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');
const cloneOfferWithHelpers = require('autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers');
const { getBunkerMock } = require('autoru-frontend/mockData/state/bunker.mock');
const panoramaExteriorMock = require('auto-core/models/panoramaExterior/mocks').default;
const panoramaInteriorMock = require('auto-core/models/panoramaInterior/mocks').default;

let props;
let initialState;
const eventMap = {};
let originalWindowPostMessage;

let consoleErrorOriginal;

beforeEach(() => {
    initialState = {
        card: cloneOfferWithHelpers(cardStateMock).withPanoramaExterior().value(),
        config: {
            data: {
                refererFromHeader: {
                    host: 'foo.bar',
                    protocol: 'http:',
                },
            },
        },
        bunker: getBunkerMock([ 'embedded/panorama' ]),
    };
    props = {};

    consoleErrorOriginal = console.error;
    originalWindowPostMessage = global.postMessage;

    /* eslint no-console: 0*/
    console.error = jest.fn();
    global.postMessage = jest.fn();

    jest.spyOn(global, 'addEventListener').mockImplementation((event, cb) => {
        eventMap[event] = cb;
    });
});

afterEach(() => {
    /* eslint no-console: 0*/
    console.error = consoleErrorOriginal;
    global.postMessage = originalWindowPostMessage;

    jest.restoreAllMocks();
});

it('если есть только экстерьерная панорама нарисует ее без табиков', () => {
    const page = shallowRenderComponent({ initialState, props });
    expect(page.find('Connect(PanoramaExterior)').isEmptyRender()).toBe(false);
    expect(page.find('.PanoramaEmbedded__tabs').isEmptyRender()).toBe(true);
});

it('если есть только интерьерная панорама нарисует ее без табиков', () => {
    initialState.card = cloneOfferWithHelpers(cardStateMock)
        .withPanoramaInterior()
        .value();

    const page = shallowRenderComponent({ initialState, props });
    expect(page.find('Connect(PanoramaInterior)').isEmptyRender()).toBe(false);
    expect(page.find('.PanoramaEmbedded__tabs').isEmptyRender()).toBe(true);
});

it('если есть две панорамы нарисует табики', () => {
    initialState.card = cloneOfferWithHelpers(cardStateMock)
        .withPanoramaInterior()
        .withPanoramaExterior()
        .value();

    const page = shallowRenderComponent({ initialState, props });
    expect(page.find('.PanoramaEmbedded__tabs').isEmptyRender()).toBe(false);
});

describe('ничего не нарисует', () => {
    it('если оффер не активен', () => {
        initialState.card = cloneOfferWithHelpers(cardStateMock).withPanoramaExterior().withStatus('INACTIVE').value();
        const page = shallowRenderComponent({ initialState, props });
        expect(page.find('.PanoramaEmbedded').isEmptyRender()).toBe(true);
        expect(console.error.mock.calls).toMatchSnapshot();
    });

    it('если оффера нет', () => {
        initialState.card = {};
        const page = shallowRenderComponent({ initialState, props });
        expect(page.find('.PanoramaEmbedded').isEmptyRender()).toBe(true);
        expect(console.error.mock.calls).toMatchSnapshot();
    });

    describe('если нет интерьерной панорамы', () => {
        it('и экстерьерная панорама в процессе обработки', () => {
            initialState.card.state.external_panorama.next = { status: 'PROCESSING' };
            const page = shallowRenderComponent({ initialState, props });
            expect(page.find('.PanoramaEmbedded').isEmptyRender()).toBe(true);
            expect(console.error.mock.calls).toMatchSnapshot();
        });

        it('и экстерьерная панорама зафейлилась', () => {
            initialState.card.state.external_panorama.published.status = 'FAILED';
            const page = shallowRenderComponent({ initialState, props });
            expect(page.find('.PanoramaEmbedded').isEmptyRender()).toBe(true);
            expect(console.error.mock.calls).toMatchSnapshot();
        });

        it('и экстерьерной панорамы тоже нет', () => {
            initialState.card = cardStateMock;
            const page = shallowRenderComponent({ initialState, props });
            expect(page.find('.PanoramaEmbedded').isEmptyRender()).toBe(true);
            expect(console.error.mock.calls).toMatchSnapshot();
        });
    });

    describe('если нет экстерьерной панорамы', () => {
        beforeEach(() => {
            initialState.card = cloneOfferWithHelpers(cardStateMock).withPanoramaInterior().value();
        });

        it('и интерьерная панорама в процессе обработки', () => {
            initialState.card.state.interior_panorama.panoramas[0].status = 'PROCESSING';
            const page = shallowRenderComponent({ initialState, props });
            expect(page.find('.PanoramaEmbedded').isEmptyRender()).toBe(true);
            expect(console.error.mock.calls).toMatchSnapshot();
        });

        it('и интерьерная панорама зафейлилась', () => {
            initialState.card.state.interior_panorama.panoramas[0].status = 'FAILED';
            const page = shallowRenderComponent({ initialState, props });
            expect(page.find('.PanoramaEmbedded').isEmptyRender()).toBe(true);
            expect(console.error.mock.calls).toMatchSnapshot();
        });

        it('и интерьерной панорамы тоже нет', () => {
            initialState.card = cardStateMock;
            const page = shallowRenderComponent({ initialState, props });
            expect(page.find('.PanoramaEmbedded').isEmptyRender()).toBe(true);
            expect(console.error.mock.calls).toMatchSnapshot();
        });
    });

    it('если обе панорамы в процессе обработки', () => {
        initialState.card = cloneOfferWithHelpers(cardStateMock)
            .withPanoramaInterior([ panoramaInteriorMock.withProcessing().value() ])
            .withPanoramaExterior('next', panoramaExteriorMock.withProcessing().value())
            .value();

        const page = shallowRenderComponent({ initialState, props });
        expect(page.find('.PanoramaEmbedded').isEmptyRender()).toBe(true);
        expect(console.error.mock.calls).toMatchSnapshot();
    });

    it('если обе панорамы сфейлились', () => {
        initialState.card = cloneOfferWithHelpers(cardStateMock)
            .withPanoramaInterior([ panoramaInteriorMock.withFailed().value() ])
            .withPanoramaExterior('next', panoramaExteriorMock.withFailed().value())
            .value();

        const page = shallowRenderComponent({ initialState, props });
        expect(page.find('.PanoramaEmbedded').isEmptyRender()).toBe(true);
        expect(console.error.mock.calls).toMatchSnapshot();
    });
});

describe('при получении ответа от верхнего фрейма', () => {
    let page;

    beforeEach(() => {
        page = shallowRenderComponent({ initialState, props });
    });

    it('если его origin находится в списке разрешенных, отправит сообщение о готовности', () => {
        eventMap.message({ data: { source: 'auto_ru_panorama_user', type: 'pong' }, origin: 'https://foo.bar' });
        expect(global.postMessage).toHaveBeenCalledTimes(2);
        expect(global.postMessage.mock.calls[1]).toMatchSnapshot();
    });

    it('если его origin не находится в списке разрешенных, скроет панораму', () => {
        eventMap.message({ data: { source: 'auto_ru_panorama_user', type: 'pong' }, origin: 'https://foo.baz' });
        expect(global.postMessage).toHaveBeenCalledTimes(1);
        expect(page.find('.PanoramaEmbedded').isEmptyRender()).toBe(true);
    });
});

function shallowRenderComponent({ initialState, props }) {
    const ContextProvider = createContextProvider(contextMock);
    const store = mockStore(initialState);

    return shallow(
        <ContextProvider>
            <PanoramaEmbedded { ...props } store={ store }/>
        </ContextProvider>,
    ).dive().dive();
}
