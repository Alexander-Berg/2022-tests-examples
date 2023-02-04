const React = require('react');
const { Provider } = require('react-redux');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
import { render, screen } from '@testing-library/react';

const contextMock = require('autoru-frontend/mocks/contextMock').default;
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;

const CardSellerNamePlaceDumb = require('./CardSellerNamePlace');

const mockFunction = () => '';

const initialState = {
    publicUserInfo: {
        data: {
            alias: 'Hulio',
            registration_date: '2022-01-23',
            offers_stats_by_category: {
                ALL: {
                    active_offers_count: 13,
                    inactive_offers_count: 0,
                },
            },
        },
    },
};

describe('частник', () => {
    it('должен отрендерить компонент', () => {
        const store = mockStore(initialState);
        const offer = {
            category: 'cars',
            section: 'used',
            seller: {
                name: 'Рудольф',
            },
            seller_type: 'PRIVATE',
        };

        const wrapper = shallow(
            <CardSellerNamePlaceDumb
                offer={ offer }
                sendMarketingEvent={ mockFunction }
            />,
            { context: { ...contextMock, store } },
        ).dive();
        expect(shallowToJson(wrapper)).toMatchSnapshot();
    });

    it('должен отрендерить ссылку на публичный профиль перекупа', () => {
        const store = mockStore(initialState);
        const offer = {
            category: 'cars',
            section: 'used',
            seller: {
                name: 'Рудольф',
            },
            seller_type: 'PRIVATE',
            additional_info: {
                other_offers_show_info: { encrypted_user_id: 'some_encrypted_id' },
            },
        };
        const Context = createContextProvider(contextMock);

        render(
            <Provider store={ store }>
                <Context>
                    <CardSellerNamePlaceDumb offer={ offer } sendMarketingEvent={ mockFunction }/>
                </Context>
            </Provider>,
        );

        const link = screen.getByRole('link', { name: /рудольф/i });

        expect(link.getAttribute('href')).toEqual('link/reseller-public-page/?encrypted_user_id=some_encrypted_id');
    });
});

describe('дилер', () => {
    let offer;
    beforeEach(() => {
        offer = {
            category: 'cars',
            sub_category: 'cars',
            salon: {
                code: 'autocenter',
            },
            section: 'new',
            seller: {
                location: {
                    region_info: { name: 'Москва' },
                },
                name: 'автоцентр',
            },
            seller_type: 'COMMERCIAL',
        };
    });

    it('должен отрендерить компонент для оф. дилера новых авто', () => {
        const store = mockStore(initialState);
        offer.salon.is_official = true;
        offer.category = 'cars';

        const wrapper = shallow(
            <CardSellerNamePlaceDumb
                offer={ offer }
                sendMarketingEvent={ mockFunction }
            />,
            { context: { ...contextMock, store } },
        ).dive;
        expect(shallowToJson(wrapper)).toMatchSnapshot();
    });

    it('должен отрендерить компонент для оф. дилера новых мото', () => {
        const store = mockStore(initialState);
        offer.salon.is_official = true;
        offer.category = 'moto';

        const wrapper = shallow(
            <CardSellerNamePlaceDumb
                offer={ offer }
                sendMarketingEvent={ mockFunction }
            />,
            { context: { ...contextMock, store } },
        ).dive();
        expect(shallowToJson(wrapper)).toMatchSnapshot();
    });

    it('должен отрендерить компонент для неоф. дилера новых', () => {
        const store = mockStore(initialState);
        offer.salon.is_official = false;
        offer.section = 'new';

        const wrapper = shallow(
            <CardSellerNamePlaceDumb
                offer={ offer }
                sendMarketingEvent={ mockFunction }
            />,
            { context: { ...contextMock, store } },
        ).dive();
        expect(shallowToJson(wrapper)).toMatchSnapshot();
    });

    it('должен отрендерить компонент дилера бу', () => {
        const store = mockStore(initialState);
        offer.section = 'used';

        const wrapper = shallow(
            <CardSellerNamePlaceDumb
                offer={ offer }
                sendMarketingEvent={ mockFunction }
            />,
            { context: { ...contextMock, store } },
        ).dive();
        expect(shallowToJson(wrapper)).toMatchSnapshot();
    });

    it('должен отрендерить текст вместо ссылки если не указан salon.code', () => {
        const store = mockStore(initialState);
        offer.section = 'used';
        offer.salon.code = undefined;

        const wrapper = shallow(
            <CardSellerNamePlaceDumb
                offer={ offer }
                sendMarketingEvent={ mockFunction }
            />,
            { context: { ...contextMock, store } },
        ).dive();
        expect(shallowToJson(wrapper)).toMatchSnapshot();
    });

    it('правильно формирует ссылку на кол-во доступных предложений', () => {
        const store = mockStore(initialState);
        offer.salon.is_official = true;
        offer.salon.offer_counters = {
            cars_all: 42,
        };
        offer.category = 'cars';

        const wrapper = shallow(
            <CardSellerNamePlaceDumb
                offer={ offer }
                sendMarketingEvent={ mockFunction }
            />,
            { context: { ...contextMock, store } },
        ).dive();

        const link = wrapper.find('.CardSellerNamePlace__count');
        expect(shallowToJson(link)).toMatchSnapshot();
    });

});
