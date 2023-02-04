const _ = require('lodash');
const React = require('react');
const { shallow } = require('enzyme');
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const cloneOfferWithHelpers = require('autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers');

const IndexPresetsOffer = require('./IndexPresetsOffer');
const MetrikaLink = require('../../MetrikaLink');
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const offerMock = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');

let props;
let context;
const defaultProps = {
    offer: offerMock,
    presetId: 'vin-history',
    isPersonalized: false,
};

beforeEach(() => {
    context = _.cloneDeep(contextMock);
});

describe('правильно формирует параметры метрики', () => {
    beforeEach(() => {
        props = _.cloneDeep(defaultProps);
    });

    it('если поиск был не персонализированный', () => {
        props.isPersonalized = false;

        const page = shallowRenderIndexPresetsOffer(props);
        const metrikaComponent = page.find(MetrikaLink);

        expect(metrikaComponent.prop('metrika')).toBe('presets,vin-history');
    });

    it('если поиск был персонализированный', () => {
        props.isPersonalized = true;

        const page = shallowRenderIndexPresetsOffer(props);
        const metrikaComponent = page.find(MetrikaLink);

        expect(metrikaComponent.prop('metrika')).toBe('presets,custom,vin-history');
    });
});

describe('должен отрендерить оффер новых', () => {
    let props;
    beforeEach(() => {
        props = {
            ..._.cloneDeep(defaultProps),
            offer: cloneOfferWithHelpers(offerMock).withSection('new').withDiscountOptions({
                max_discount: 42000,
            }).value(),
        };
    });

    it('с оригинальной ценой без скидки, если не передан параметр showDiscount', () => {
        const tree = shallowRenderIndexPresetsOffer(props);
        expect(tree.find('.IndexPresets__offer-price').children().text()).toEqual('855 000 ₽');
        expect(tree.find('.IndexPresets__offer-price-original')).not.toExist();
    });

    it('с оригинальной ценой без скидки, если нет скидки', () => {
        const offer = cloneOfferWithHelpers(offerMock).withSection('new').withDiscountOptions(undefined).value();
        const tree = shallowRenderIndexPresetsOffer({
            ...props,
            showDiscount: true,
            offer,
        });
        expect(tree.find('.IndexPresets__offer-price').children().text()).toEqual('855 000 ₽');
        expect(tree.find('.IndexPresets__offer-price-original')).not.toExist();
    });

    it('с ценой со скидкой и оригинальной ценой, если есть скидка', () => {
        const tree = shallowRenderIndexPresetsOffer({
            ...props,
            showDiscount: true,
        });
        expect(tree.find('.IndexPresets__offer-price-original')).toExist();
    });
});

function shallowRenderIndexPresetsOffer(props) {
    const ContextProvider = createContextProvider(context);

    const wrapper = shallow(
        <ContextProvider>
            <IndexPresetsOffer { ...props } searchID="123"/>
        </ContextProvider>,
    );

    return wrapper.dive();
}
