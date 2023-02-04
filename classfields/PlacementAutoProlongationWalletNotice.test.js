const _ = require('lodash');
const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const PlacementAutoProlongationWalletNotice = require('./PlacementAutoProlongationWalletNotice');
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const configStateMock = require('auto-core/react/dataDomain/config/mock').default;

let props;
let context;
let initialState;

beforeEach(() => {
    initialState = {
        user: { data: {} },
        config: configStateMock.value(),
    };
    props = {
        offers: [ {
            services: [ { service: 'all_sale_activate', prolongable: true, is_active: true } ],
            service_prices: [ { service: 'all_sale_activate', prolongation_forced_not_togglable: true } ],
            additional_info: { expire_date: 1561965602587 },
            status: 'ACTIVE',
        } ],
        metrikaFromParam: 'from-lk',
    };
    context = _.cloneDeep(contextMock);

    context.metrika.sendParams.mockClear();
});

describe('если есть автопродляемое размещение, оно активно и у пользователя нет привязанных карт', () => {
    let page;

    beforeEach(() => {
        page = shallowRenderComponent(props, context, initialState);
    });

    it('правильно рисует компонент', () => {
        expect(shallowToJson(page)).toMatchSnapshot();
    });

    it('отправляет метрики показов', () => {
        expect(context.metrika.sendParams).toHaveBeenCalledTimes(2);
        expect(context.metrika.sendParams.mock.calls[0][0]).toEqual([ '7days-placement', 'wallet-refill', 'shows', 'from-lk' ]);
        expect(context.metrika.sendParams.mock.calls[1][0]).toEqual([ '7days-placement', 'landing-page', 'shows', 'from-lk' ]);
    });

    it('при клике на ссылку "подробнее" отправит метрику', () => {
        const link = page.find('Link');
        link.simulate('click');

        expect(context.metrika.sendParams).toHaveBeenCalledTimes(3);
        expect(context.metrika.sendParams.mock.calls[2][0]).toEqual([ '7days-placement', 'landing-page', 'clicks', 'from-lk' ]);
    });

    it('при клике на кнопку "пополнить кошелёк" отправит метрику', () => {
        const button = page.find('Button');
        button.simulate('click');

        expect(context.metrika.sendParams).toHaveBeenCalledTimes(3);
        expect(context.metrika.sendParams.mock.calls[2][0]).toEqual([ '7days-placement', 'wallet-refill', 'clicks', 'from-lk' ]);
    });
});

it('правильно рисует компонент для мобилки', () => {
    props.isMobile = true;
    const page = shallowRenderComponent(props, context, initialState);
    expect(shallowToJson(page)).toMatchSnapshot();
});

describe('ничего не нарисует', () => {
    it('если активация неактивна', () => {
        props.offers[0].services[0].is_active = false;
        const page = shallowRenderComponent(props, context, initialState);

        expect(page.html()).toBeNull();
    });

    it('если автопродление не подключено', () => {
        props.offers[0].services[0].prolongable = false;
        const page = shallowRenderComponent(props, context, initialState);

        expect(page.html()).toBeNull();
    });

    it('если автопродление не форсируется', () => {
        props.offers[0].service_prices[0].prolongation_forced_not_togglable = false;
        const page = shallowRenderComponent(props, context, initialState);

        expect(page.html()).toBeNull();
    });

    it('если у пользователя есть привязанная карта', () => {
        initialState.user.data.tied_cards = [ { id: '1' } ];
        const page = shallowRenderComponent(props, context, initialState);

        expect(page.html()).toBeNull();
    });

    it('если статус объявления INACTIVE', () => {
        props.offers[0].status = 'INACTIVE';
        const page = shallowRenderComponent(props, context, initialState);

        expect(page.html()).toBeNull();
    });
});

function shallowRenderComponent(props, context, initialState) {
    const store = mockStore(initialState);
    const ContextProvider = createContextProvider(context);

    return shallow(
        <ContextProvider>
            <PlacementAutoProlongationWalletNotice { ...props } store={ store }/>
        </ContextProvider>,
    ).dive().dive();
}
