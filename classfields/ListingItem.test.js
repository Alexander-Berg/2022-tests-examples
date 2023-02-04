/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

const _ = require('lodash');
const React = require('react');
const { Provider } = require('react-redux');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const mockdate = require('mockdate');
const { render, screen } = require('@testing-library/react');
const userEvent = require('@testing-library/user-event').default;
const { mockAllIsIntersecting } = require('react-intersection-observer/test-utils');

import '@testing-library/jest-dom';

const card = require('auto-core/react/dataDomain/listing/mocks/listingOffer.cars.mock').default;
const cardMotorcycle = require('auto-core/react/dataDomain/card/mocks/card.motorcycle.mock');

const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const cloneOfferWithHelpers = require('autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers');

const ListingItem = require('./ListingItem').default;

let offer;
let store;
beforeEach(() => {
    offer = _.cloneDeep(card);
    store = mockStore({});
});

describe('генерация урла на карточку', () => {
    let offer;
    let wrapper;

    beforeAll(() => {
        offer = _.cloneDeep(card);
        wrapper = shallow(
            <ListingItem
                offer={ offer }
                params={{ }}
                sendMarketingEventByListingOffer={ _.noop }
            />,
            { context: { ...contextMock, store } },
        );
    });

    it('должен генерировать ссылку на карточку б/у, если это оффер б/у', () => {
        wrapper.setProps({ offer });
        const instance = wrapper.instance();
        const url = instance.getCardUrl();
        expect(url).toBe('link/card/?category=cars&section=used&mark=FORD&model=ECOSPORT&sale_id=1085562758&sale_hash=1970f439&from=');
    });

    it('должен генерировать ссылку на оффер новых, если это оффер новых', () => {
        offer.section = 'new';
        wrapper.setProps({ offer });
        const instance = wrapper.instance();
        const url = instance.getCardUrl();
        // eslint-disable-next-line max-len
        expect(url).toBe('link/card/?category=cars&section=new&mark=FORD&model=ECOSPORT&sale_id=1085562758&sale_hash=1970f439&tech_param_id=20104325&complectation_id=0&from=');
    });
});

describe('ListingItemSalonName', () => {
    it('должен отображать название дилера и метро', () => {
        offer.section = 'new';
        offer.salon = {
            is_official: true,
        };
        offer.seller = {
            name: 'БорисХоф BMW Восток',
        };

        const wrapper = shallow(
            <ListingItem
                offer={ offer }
                params={{ }}
                sendMarketingEventByListingOffer={ _.noop }
            />,
            { context: { ...contextMock, store } },
        );
        expect(wrapper.find('ListingItemSalonName')).not.toBeEmptyRender();
        expect(wrapper.find('MetroListPlace')).not.toBeEmptyRender();
    });
});

describe('бейдж про эксклюзивный оффер', () => {
    it('добавит бейдж если у оффера есть тэг', () => {
        offer.tags = [ 'autoru_exclusive' ];

        const wrapper = shallow(
            <ListingItem
                offer={ offer }
                params={{}}
                sendMarketingEventByListingOffer={ _.noop }
            />,
            { context: { ...contextMock, store } },
        );
        expect(wrapper.find('ListingItemTagsDesktop').dive().find('BadgeForExclusiveOfferMobile')).not.toBeEmptyRender();
    });

    it('не добавит бейдж если у оффера нет тэга', () => {
        const wrapper = shallow(
            <ListingItem
                offer={ offer }
                params={{}}
                sendMarketingEventByListingOffer={ _.noop }
            />,
            { context: { ...contextMock, store } },
        );
        expect(wrapper.find('ListingItemTagsDesktop').dive().find('BadgeForExclusiveOfferMobile')).not.toExist();
    });
});

it('НЕ должен рендерить кнопки действий при наведении на проданный оффер', async() => {
    const soldOffer = _.cloneDeep(offer);
    soldOffer.status = 'INACTIVE';

    const wrapper = shallow(
        <ListingItem
            offer={ soldOffer }
            params={{}}
            sendMarketingEventByListingOffer={ _.noop }
        />,
        { context: { ...contextMock, store } },
    );
    expect(wrapper.find('ListingItemActions')).not.toExist();
});

it('не должен рендерить бейджи на проданном оффере', async() => {
    const soldOffer = _.cloneDeep(offer);
    soldOffer.status = 'INACTIVE';

    const wrapper = shallow(
        <ListingItem
            offer={ soldOffer }
            params={{}}
            sendMarketingEventByListingOffer={ _.noop }
        />,
        { context: { ...contextMock, store } },
    );
    expect(wrapper.find('ListingItemTagsDesktop')).not.toExist();
});

describe('extendedLinks', () => {
    let motoOffer;

    beforeEach(() => {
        mockdate.set('2020-07-01');
        motoOffer = _.cloneDeep(cardMotorcycle);
    });

    afterEach(() => {
        mockdate.reset();
    });

    it('для cars', () => {
        offer.section = 'new';
        offer.salon = {
            code: 'salon-code',
            is_official: true,
            registration_date: '1984-07-27',
        };
        offer.services = [ { service: 'all_sale_premium', is_active: true } ];
        offer.seller_type = 'COMMERCIAL';

        const linksWrapper = shallow(
            <ListingItem
                offer={ offer }
                params={{}}
                sendMarketingEventByListingOffer={ _.noop }
            />,
            { context: { ...contextMock, store } },
        ).find('.ListingItem__extendedLabels');

        expect(shallowToJson(linksWrapper)).toMatchSnapshot();
    });

    it('для moto', () => {
        motoOffer.section = 'new';
        motoOffer.salon = {
            code: 'salon-code',
            is_official: true,
            registration_date: '1984-07-27',
        };
        motoOffer.services = [ { service: 'all_sale_premium', is_active: true } ];
        motoOffer.seller_type = 'COMMERCIAL';

        const linksWrapper = shallow(
            <ListingItem
                offer={ motoOffer }
                params={{}}
                sendMarketingEventByListingOffer={ _.noop }
            />,
            { context: { ...contextMock, store } },
        ).find('.ListingItem__extendedLabels');

        expect(shallowToJson(linksWrapper)).toMatchSnapshot();
    });
});

describe('метрика для айтема с панорамой', () => {
    it('отправляется для элемента с вращением по скролу когда заголовок попадает в зону видимости', () => {
        const offer = cloneOfferWithHelpers(card)
            .withPanoramaExterior()
            .withActiveVas([ 'all_sale_toplist' ])
            .value();
        const wrapper = shallow(
            <ListingItem
                offer={ offer }
                params={{}}
                index={ 1 }
                sendMarketingEventByListingOffer={ _.noop }
            />,
            { context: { ...contextMock, store } },
        );
        const itemTitle = wrapper.find('ListingItemTitle');
        itemTitle.simulate('intersectionChange', true);

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
        expect(contextMock.metrika.sendPageEvent.mock.calls[0]).toMatchSnapshot();
    });

    it('отправляется для элемента без вращения по скролу когда заголовок попадает в зону видимости', () => {
        const offer = cloneOfferWithHelpers(card)
            .withActiveVas([])
            .withPanoramaExterior()
            .value();
        const wrapper = shallow(
            <ListingItem
                offer={ offer }
                params={{}}
                sendMarketingEventByListingOffer={ _.noop }
            />,
            { context: { ...contextMock, store } },
        );
        const itemTitle = wrapper.find('ListingItemTitle');
        itemTitle.simulate('intersectionChange', true);

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
        expect(contextMock.metrika.sendPageEvent.mock.calls[0]).toMatchSnapshot();
    });

    it('отправляется для внутренней панорамы, если нет внешней', () => {
        const offer = cloneOfferWithHelpers(card)
            .withActiveVas([])
            .withPanoramaInterior()
            .value();
        const wrapper = shallow(
            <ListingItem
                offer={ offer }
                params={{}}
                sendMarketingEventByListingOffer={ _.noop }
            />,
            { context: { ...contextMock, store } },
        );
        const itemTitle = wrapper.find('ListingItemTitle');
        itemTitle.simulate('intersectionChange', true);

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
        expect(contextMock.metrika.sendPageEvent.mock.calls[0]).toMatchSnapshot();
    });

    it('не отправляется для элемента без панорамы', () => {
        const wrapper = shallow(
            <ListingItem
                offer={ card }
                params={{}}
                sendMarketingEventByListingOffer={ _.noop }
            />,
            { context: { ...contextMock, store } },
        );
        const itemTitle = wrapper.find('ListingItemTitle');
        itemTitle.simulate('intersectionChange', true);

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(0);
    });

    it('не отправляется если элемент не виден', () => {
        const offer = cloneOfferWithHelpers(card)
            .withActiveVas([])
            .withPanoramaExterior()
            .value();
        const wrapper = shallow(
            <ListingItem
                offer={ offer }
                params={{}}
                sendMarketingEventByListingOffer={ _.noop }
            />,
            { context: { ...contextMock, store } },
        );
        const itemTitle = wrapper.find('ListingItemTitle');
        itemTitle.simulate('intersectionChange', false);

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(0);
    });
});

describe('ссылка на публичный профиль перекупа', () => {
    it('не рендерит ссылку на публичный профиль перекупа, если нет encrypted_user_id', () => {
        const Context = createContextProvider(contextMock);
        render(
            <Provider store={ store }>
                <Context>
                    <ListingItem
                        offer={ offer }
                        params={{}}
                        sendMarketingEventByListingOffer={ _.noop }
                    />
                </Context>
            </Provider>,
        );

        const link = screen.queryByRole('link', { name: /дмитрий/i });

        expect(link).not.toBeInTheDocument();
    });

    it('рендерит ссылку на публичный профиль перекупа и отправляет метрику по клику, если пришел encrypted_user_id', () => {
        const Context = createContextProvider(contextMock);
        const offerMock = cloneOfferWithHelpers(offer).withEncryptedUserId('some_encrypted_id').value();
        render(
            <Provider store={ store }>
                <Context>
                    <ListingItem
                        offer={ offerMock }
                        params={{}}
                        sendMarketingEventByListingOffer={ _.noop }
                    />
                </Context>
            </Provider>,
        );

        const link = screen.getByRole('link', { name: /дмитрий/i });
        userEvent.click(link);

        expect(link.getAttribute('href')).toBe('link/reseller-public-page/?encrypted_user_id=some_encrypted_id');
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'reseller_public', 'link-click' ]);
    });

    it('отправляет метрику при попадании ссылки в зону видимости', () => {
        const Context = createContextProvider(contextMock);
        const offerMock = cloneOfferWithHelpers(offer).withEncryptedUserId('some_encrypted_id').value();
        render(
            <Provider store={ store }>
                <Context>
                    <ListingItem
                        offer={ offerMock }
                        params={{}}
                        sendMarketingEventByListingOffer={ _.noop }
                    />
                </Context>
            </Provider>,
        );
        mockAllIsIntersecting(true);

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'reseller_public', 'link-show' ]);
    });
});
