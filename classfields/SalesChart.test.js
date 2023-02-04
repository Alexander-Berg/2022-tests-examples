const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const SalesChart = require('./SalesChart');
import DateMock from 'autoru-frontend/mocks/components/DateMock';

const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const offerMock = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');
const cloneOfferWithHelpers = require('autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers');

let props;

beforeEach(() => {
    props = {
        offer: offerMock,
        onVasSubmit: jest.fn(),
    };
});

it('должен правильно добавить иконку услуги', () => {
    props.data = [ {
        date: '2021-04-15',
        views: 25,
        phone_calls: 0,
        phone_views: 0,
        services: [
            { service: 'all_sale_color' },
        ],
    },
    {
        date: '2021-04-16',
        views: 25,
        phone_calls: 0,
        phone_views: 0,
        services: [
            { service: 'all_sale_toplist' },
        ],
    } ];
    const wrapper = shallowRenderComponent({ props });
    expect(wrapper.find('.SalesChart__infoIcon')).toHaveLength(2);
    expect(wrapper.find('.SalesChart__infoTooltip').at(0).props().tooltipContent).toEqual('Выделение цветом');
});

it('не должен дублировать иконку услуги', () => {
    props.data = [
        {
            date: '2021-04-15',
            views: 25,
            phone_calls: 0,
            phone_views: 0,
            services: [
                { service: 'all_sale_color' },
            ],
        },
        {
            date: '2021-04-16',
            views: 25,
            phone_calls: 0,
            phone_views: 0,
            services: [
                { service: 'all_sale_color' },
            ],
        } ];
    const wrapper = shallowRenderComponent({ props });
    expect(wrapper.find('.SalesChart__infoIcon')).toHaveLength(1);
});

it('должен правильно отрендерить иконку нескольких услуг', () => {
    props.data = [
        {
            date: '2021-04-15',
            views: 25,
            phone_calls: 0,
            phone_views: 0,
            services: [
                { service: 'all_sale_color' },
                { service: 'all_sale_fresh' },
                { service: 'price_change' },
            ],
        } ];
    const wrapper = shallowRenderComponent({ props });
    expect(wrapper.find('.SalesChart__infoIcon')).toHaveLength(1);
    expect(wrapper.find('.SalesChart__iconDigit')).toHaveLength(1);
    expect(shallowToJson(wrapper.find('.SalesChart__iconDigit').children())).toBe(3);

});

it('должен правильно отрендерить статистику с изменением цены', () => {
    props.data = [ {
        date: '2021-04-15',
        views: 25,
        phone_calls: 0,
        phone_views: 0,
        price_diff: -5000,
    },
    {
        date: '2021-04-16',
        views: 25,
        phone_calls: 0,
        phone_views: 0,
        price_diff: 5000,
    } ];
    const wrapper = shallowRenderComponent({ props });
    expect(wrapper.find('.SalesChart__iconDigit')).toHaveLength(2);
    expect(wrapper.find('.SalesChart__infoTooltip').at(0).props().tooltipContent).toEqual('Изменена цена: -5 000 ₽');
    expect(wrapper.find('.SalesChart__infoTooltip').at(1).props().tooltipContent).toEqual('Изменена цена: +5 000 ₽');
});

it('должен правильно расчитывать высоту столбцов', () => {
    props.data = [ {
        date: '2021-04-15',
        views: 20,
        phone_calls: 0,
        phone_views: 0,
        actions: [],
    },
    {
        date: '2021-04-16',
        views: 4,
        phone_calls: 0,
        phone_views: 0,
        actions: [],
    },
    {
        date: '2021-04-17',
        views: 0,
        phone_calls: 0,
        phone_views: 0,
        actions: [],
    } ];
    const wrapper = shallowRenderComponent({ props });
    expect(wrapper.find('.SalesChart__column').at(0).props().style.height).toEqual('100%');
    expect(wrapper.find('.SalesChart__column').at(1).props().style.height).toEqual('20%');
    expect(wrapper.find('.SalesChart__column').at(2).props().style.height).toEqual('1%');
});

describe('промка в тултипе', () => {
    const dataWithPromo = [ {
        date: '2021-04-15',
        views: 20,
        phone_calls: 0,
        phone_views: 0,
    },
    {
        date: '2021-04-16',
        views: 42,
        phone_calls: 0,
        phone_views: 0,
    },
    {
        date: '2021-04-17',
        views: 10,
        phone_calls: 0,
        phone_views: 0,
        has_promo: true,
    } ];

    function simulatePromoTooltipOpen(wrapper) {
        wrapper.setProps({ data: dataWithPromo });
    }

    beforeEach(() => {
        props.data = [];
    });

    // не можем проверить скриншотными тестами, проверяем так
    it('нарисует промо тултип', () => {
        const wrapper = shallowRenderComponent({ props });
        simulatePromoTooltipOpen(wrapper);
        const promoTooltip = wrapper.find('.SalesChart__promoTooltip');

        expect(shallowToJson(promoTooltip)).toMatchSnapshot();

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
        expect(contextMock.metrika.sendPageEvent.mock.calls[0]).toMatchSnapshot();
    });

    it('не нарисует промо тултип если у оффера первая позиция в поиске', () => {
        props.offer = cloneOfferWithHelpers(offerMock).withSearchPosition(1);
        const wrapper = shallowRenderComponent({ props });
        simulatePromoTooltipOpen(wrapper);
        const promoTooltip = wrapper.find('.SalesChart__promoTooltip');

        expect(promoTooltip.isEmptyRender()).toBe(true);

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(0);
    });

    describe('при открытии тултипа про сегодня', () => {
        let todaysTooltip;
        let wrapper;

        beforeEach(() => {
            wrapper = shallowRenderComponent({ props });
            simulatePromoTooltipOpen(wrapper);
            todaysTooltip = wrapper.find('HoveredTooltip').find({ className: 'SalesChart__tooltip' }).at(2);
            todaysTooltip.simulate('open');
        });

        it('залогирует показ кнопки', () => {
            expect(contextMock.logVasEvent).toHaveBeenCalledTimes(1);
            expect(contextMock.logVasEvent.mock.calls[0]).toMatchSnapshot();
        });

        it('отправит метрику ховера на тултип', () => {
            expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(2);
            expect(contextMock.metrika.sendPageEvent.mock.calls[1]).toMatchSnapshot();
        });

        it('прокинет флаг видимиости в тултип', () => {
            expect(todaysTooltip.prop('isVisible')).toBe(false);

            const updatedTodaysTooltip = wrapper.find('HoveredTooltip').find({ className: 'SalesChart__tooltip' }).at(2);
            expect(updatedTodaysTooltip.prop('isVisible')).toBe(true);
        });

        it('скроет промо', () => {
            const promoTooltip = wrapper.find('.SalesChart__promoTooltip');

            expect(promoTooltip.isEmptyRender()).toBe(true);
        });
    });

    it('при открытии тултипа не про сегодня, скроет промо тултип', () => {
        const wrapper = shallowRenderComponent({ props });
        simulatePromoTooltipOpen(wrapper);
        const notTodaysTooltip = wrapper.find('HoveredTooltip').find({ className: 'SalesChart__tooltip' }).at(0);
        notTodaysTooltip.simulate('open');

        const promoTooltip = wrapper.find('.SalesChart__promoTooltip');
        expect(promoTooltip.isEmptyRender()).toBe(true);
    });

    it('при закрытии тултипа про сегодня, скроет его и промо тултип', () => {
        const wrapper = shallowRenderComponent({ props });
        simulatePromoTooltipOpen(wrapper);
        const todaysTooltip = wrapper.find('HoveredTooltip').find({ className: 'SalesChart__tooltip' }).at(2);
        todaysTooltip.simulate('open');
        todaysTooltip.simulate('close');

        const updatedTodaysTooltip = wrapper.find('HoveredTooltip').find({ className: 'SalesChart__tooltip' }).at(2);
        expect(updatedTodaysTooltip.prop('isVisible')).toBe(false);

        const promoTooltip = wrapper.find('.SalesChart__promoTooltip');
        expect(promoTooltip.isEmptyRender()).toBe(true);
    });
});

function shallowRenderComponent({ props }) {
    const ContextProvider = createContextProvider(contextMock);

    return shallow(
        <ContextProvider>
            <DateMock date="2021-04-17">
                <SalesChart { ...props }/>
            </DateMock>
        </ContextProvider>,
    ).dive().dive();
}
