const _ = require('lodash');
const React = require('react');
const { shallow } = require('enzyme');

const SalesItemPhoto = require('./SalesItemPhoto');

const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const cardStateMock = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');
const cloneOfferWithHelpers = require('autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers');
const panoramaExteriorMock = require('auto-core/models/panoramaExterior/mocks').default;

let props;
let context;

beforeEach(() => {
    props = {
        offer: cardStateMock,
    };
    context = _.cloneDeep(contextMock);
    context.metrika.sendParams.mockClear();
});

it('правильно формирует ссылку если у оффера есть фото', () => {
    const page = shallowRenderComponent({ props, context });
    const link = page.find('a');

    expect(link.prop('href')).toMatchSnapshot();
});

it('правильно формирует ссылку если у оффера нет фото', () => {
    props.offer = cloneOfferWithHelpers(cardStateMock)
        .withCustomState({ image_urls: [] })
        .withAction({ edit: [] })
        .value();

    const page = shallowRenderComponent({ props, context });
    const link = page.find('a');

    expect(link.prop('href')).toMatchSnapshot();
});

it('правильно формирует ссылку если оффер в статусе драфт', () => {
    props.offer = cloneOfferWithHelpers(cardStateMock)
        .withStatus('DRAFT')
        .value();

    const page = shallowRenderComponent({ props, context });
    const link = page.find('a');

    expect(link.prop('href')).toMatchSnapshot();
});

it('правильно формирует ссылку если произошла ошибка обработки панорамы', () => {
    props.offer = cloneOfferWithHelpers(cardStateMock)
        .withPanoramaExterior('next', panoramaExteriorMock.withFailed().value())
        .value();

    const page = shallowRenderComponent({ props, context });
    const panoramaError = page.find('PanoramaProcessingError');

    expect(panoramaError.prop('url')).toMatchSnapshot();
});

function shallowRenderComponent({ context, props }) {
    const ContextProvider = createContextProvider(context);

    return shallow(
        <ContextProvider>
            <SalesItemPhoto { ...props }/>
        </ContextProvider>,
    ).dive();
}
