const React = require('react');
const { shallow } = require('enzyme');
const { OfferPosition_OrderedPosition_Sort: OfferPositionSort } = require('@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model');

const SalesItemGraphDump = require('./SalesItemGraphDump');

const offerMock = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');
const cloneOfferWithHelpers = require('autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers');
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;

let props;

beforeEach(() => {
    props = {
        offer: offerMock,
        stats: [],
        onVasSubmit: jest.fn(),
        hasVipTab: false,
    };
});

it('должен правильно обрабатываться клик на поднятие в топ', () => {

    const wrapper = shallowRenderComponent({ props });
    wrapper.find('Connect(SalesItemVASFresh)').simulate('submit', [ 'all_sale_fresh' ]);
    expect(props.onVasSubmit).toHaveBeenCalledTimes(1);
});

describe('позиция в поиске', () => {
    it('если данных об относительной позиции нет, не покажет ничего', () => {
        props.offer = cloneOfferWithHelpers(offerMock)
            .withSearchPositions([])
            .value();
        const page = shallowRenderComponent({ props });

        const relativePositionBlock = page.find('RelativeSearchPositionPopup');
        expect(relativePositionBlock.isEmptyRender()).toBe(true);
    });

    it('если данных об общей позиции нет, не покажет ничего', () => {
        props.offer = cloneOfferWithHelpers(offerMock)
            .withSearchPositions([ {
                positions: [
                    { position: 12, sort: OfferPositionSort.SIMPLE_RELEVANCE, total_count: 101 },
                ],
                total_count: 1,
            } ])
            .withSearchPosition(-1)
            .value();
        const page = shallowRenderComponent({ props });

        const relativePositionBlock = page.find('RelativeSearchPositionPopup');
        expect(relativePositionBlock.isEmptyRender()).toBe(true);
    });

    it('если есть данные про относительную позицию, покажет блок с корректной ссылкой', () => {
        props.offer = cloneOfferWithHelpers(offerMock)
            .withSearchPositions([ {
                positions: [
                    { position: 12, sort: OfferPositionSort.SIMPLE_RELEVANCE, total_count: 101 },
                ],
                total_count: 1,
            } ])
            .value();
        const page = shallowRenderComponent({ props });

        const block = page.find('RelativeSearchPositionPopup');
        expect(block.isEmptyRender()).toBe(false);
        expect(block.prop('linkUrl')).toBe('link/listing/?category=cars&mark=FORD&model=ECOSPORT&section=used&geo_radius=200&geo_id=213');
    });
});

function shallowRenderComponent({ context, props }) {
    const ContextProvider = createContextProvider(context || contextMock);

    const page = shallow(
        <ContextProvider>
            <SalesItemGraphDump { ...props }/>
        </ContextProvider>,
    );

    return page.dive();
}
