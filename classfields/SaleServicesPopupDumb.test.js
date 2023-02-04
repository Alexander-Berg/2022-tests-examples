const _ = require('lodash');
const React = require('react');
const SaleServicesPopupDumb = require('./SaleServicesPopupDumb');

const { shallow } = require('enzyme');
const cardMock = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;

let props;

let context;
let vasLogParams;
contextMock.logVasEvent = jest.fn((params) => {
    vasLogParams = params;
});

const ComponentMock = () => <div>Foo</div>;

beforeEach(() => {
    props = {
        children: <ComponentMock/>,
        className: '',
        directions: undefined,
        days: 3,
        info: _.find(cardMock.service_prices, { service: 'all_sale_color' }),
        type: SaleServicesPopupDumb.TYPE.COLOR,
        button: undefined,
        offerId: cardMock.saleId,
        category: cardMock.category,
        isAuth: true,
    };

    context = _.cloneDeep(contextMock);

    context.logVasEvent.mockClear();
});

describe('события васов:', () => {
    it('правильно логирует событие показа в стандартном попапе', () => {
        const page = shallowRenderComponent();
        page.simulate('showPopup', {});

        expect(context.logVasEvent).toHaveBeenCalledTimes(1);
        expect(vasLogParams).toMatchSnapshot();
    });

    it('если кнопки нет то не будет логировать показы', () => {
        props.button = null;
        const page = shallowRenderComponent();
        page.simulate('showPopup', {});

        expect(context.logVasEvent).toHaveBeenCalledTimes(0);
    });

    it('при клике на кнопку отправить соответствующее событие', () => {
        const page = shallowRenderComponent();
        page.simulate('showPopup', {});
        const button = page.wrap(page.instance().renderPopupButton());
        button.simulate('click');

        expect(context.logVasEvent).toHaveBeenCalledTimes(2);
        expect(vasLogParams).toMatchSnapshot();
    });
});

function shallowRenderComponent() {
    const ContextProvider = createContextProvider(context);

    const wrapper = shallow(
        <ContextProvider>
            <SaleServicesPopupDumb { ...props }/>
        </ContextProvider>,
    );

    return wrapper.dive();
}
