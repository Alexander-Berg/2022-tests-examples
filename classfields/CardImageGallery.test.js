const React = require('react');
const { shallow } = require('enzyme');
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;

const newCardStateMock = require('autoru-frontend/mockData/state/newCard.mock').card;
const cardStateMock = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');
const cloneOfferWithHelpers = require('autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers');
const configStateMock = require('auto-core/react/dataDomain/config/mock').default;
const panoramaExteriorMock = require('auto-core/models/panoramaExterior/mocks').default;

const CardImageGallery = require('./CardImageGallery');
const { OfferStatus } = require('@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model');

let storeMock;
beforeEach(() => {
    storeMock = {
        ads: { data: {} },
        bunker: { 'common/metrics': {} },
        config: configStateMock.value(),
        matchApplication: {},
    };
});

it('должен передавать renderSideBar для активного оффера', () => {
    const store = mockStore(storeMock);
    const context = {
        ...contextMock,
        store,
    };
    const wrapper = shallow(<CardImageGallery
        offer={ newCardStateMock }
    />, { context: context }).dive();
    const ImageGallery = wrapper.find('Connect(ImageGalleryDesktop)');
    expect(ImageGallery.props().renderSidebar).not.toBeUndefined();
});

it('не должен передавать renderSideBar для проданного оффера', () => {
    const store = mockStore(storeMock);
    const context = {
        ...contextMock,
        store,
    };
    const wrapper = shallow(<CardImageGallery
        offer={{ ...newCardStateMock, status: 'REMOVED' }}
    />, { context: context }).dive();
    const ImageGallery = wrapper.find('Connect(ImageGalleryDesktop)').dive();
    expect(ImageGallery.props().renderSidebar).toBeUndefined();
});

describe('плашка про каталожные фото', () => {
    let card;
    let context;
    beforeEach(() => {
        const store = mockStore(storeMock);
        context = {
            ...contextMock,
            store,
        };
        card = cloneOfferWithHelpers(newCardStateMock)
            .withSection('used')
            .withTags([ 'catalog_photo' ]);
    });

    it('должен нарисовать плашку для бу авто с тегом', () => {
        const wrapper = shallow(<CardImageGallery
            offer={ card.value() }
        />, { context: context }).dive();
        const gallery = wrapper.find('Connect(ImageGalleryDesktop)').dive().dive();

        expect(gallery.find('.CardImageGallery__fakeWarning')).toHaveHTML(
            '<div class="CardImageGallery__fakeWarning">Продавец не загрузил фото автомобиля, вы видите изображение из каталога</div>',
        );
    });

    it('не должен нарисовать плашку для бу авто с тегом в режиме fullscreen', () => {
        const wrapper = shallow(<CardImageGallery
            offer={ card.value() }
        />, { context: context }).dive();
        wrapper.setState({ isFullscreen: true });

        const gallery = wrapper.find('Connect(ImageGalleryDesktop)').dive().dive();

        expect(gallery.find('.CardImageGallery__fakeWarning')).not.toExist();
    });

    it('не должен нарисовать плашку для бу авто без тега', () => {
        card = card.withTags([ 'vin_resolution_ok' ]);

        const wrapper = shallow(<CardImageGallery
            offer={ card.value() }
        />, { context: context }).dive();
        const gallery = wrapper.find('Connect(ImageGalleryDesktop)').dive().dive();

        expect(gallery.find('.CardImageGallery__fakeWarning')).not.toExist();
    });

    it('не должен нарисовать плашку для нового авто', () => {
        card = card.withSection('new');
        // на самом деле такого быть не может
        // card.tags = [ 'catalog_photo' ];

        const wrapper = shallow(<CardImageGallery
            offer={ card.value() }
        />, { context: context }).dive();
        const gallery = wrapper.find('Connect(ImageGalleryDesktop)').dive().dive();

        expect(gallery.find('.CardImageGallery__fakeWarning')).not.toExist();
    });
});

describe('бейджики', () => {
    let context;
    beforeEach(() => {
        const store = mockStore(storeMock);
        context = {
            ...contextMock,
            store,
        };
    });

    it('добавить бейджик про панораму если она есть', () => {
        const card = cloneOfferWithHelpers(cardStateMock).withPanoramaExterior().value();

        const wrapper = shallow(<CardImageGallery
            offer={ card }
        />, { context: context }).dive();
        const panoramaBadge = wrapper.find('Badge[badge="360°"]');

        expect(panoramaBadge).not.toBeEmptyRender();
    });

    it('не добавить бейджик про панораму если ее нет', () => {
        const wrapper = shallow(<CardImageGallery
            offer={ cardStateMock }
        />, { context: context }).dive();
        const panoramaBadge = wrapper.find('Badge[badge="360°"]');

        expect(panoramaBadge).not.toExist();
    });

    it('добавить бейджик про панораму если есть внутренняя панорама', () => {
        const card = cloneOfferWithHelpers(cardStateMock).withPanoramaInterior().value();

        const wrapper = shallow(<CardImageGallery
            offer={ card }
        />, { context: context }).dive();
        const imageGallery = wrapper.find('Connect(ImageGalleryDesktop)').dive().dive();
        imageGallery.find('.ImageGalleryDesktop__right-nav').simulate('click');
        const panoramaBadge = wrapper.find('Badge[badge="360°"]');

        expect(panoramaBadge).not.toBeEmptyRender();
    });

    it('добавить бейджик про панораму если есть spincar', () => {
        const card = cloneOfferWithHelpers(cardStateMock).value();
        card.state.panoramas = {
            spincar_exterior_url: 'foo',
        };

        const wrapper = shallow(<CardImageGallery
            offer={ card }
        />, { context: context }).dive();
        const imageGallery = wrapper.find('Connect(ImageGalleryDesktop)').dive().dive();
        imageGallery.find('.ImageGalleryDesktop__right-nav').simulate('click');
        const panoramaBadge = wrapper.find('Badge[badge="360°"]');

        expect(panoramaBadge).not.toBeEmptyRender();
    });
});

describe('ошибка обработки панорамы', () => {
    let store;
    let context;

    beforeEach(() => {
        store = mockStore(storeMock);
        context = {
            ...contextMock,
            store,
        };
    });

    it('покажется под владельцем с правильной ссылкой если обработка упала', () => {
        const card = cloneOfferWithHelpers(cardStateMock).withIsOwner().withPanoramaExterior('next', panoramaExteriorMock.withFailed().value()).value();
        const wrapper = shallow(<CardImageGallery offer={ card }/>, { context: context }).dive();
        const gallery = wrapper.find('Connect(ImageGalleryDesktop)').dive().dive();
        const panoramaErrorElem = gallery.find('PanoramaProcessingError');

        expect(panoramaErrorElem.isEmptyRender()).toBe(false);
        expect(panoramaErrorElem.prop('url')).toBe('link/form/?category=cars&section=used&form_type=edit&sale_id=1085562758&sale_hash=1970f439#panorama');
    });

    it('не покажется под невладельцем если обработка упала', () => {
        const card = cloneOfferWithHelpers(cardStateMock).withIsOwner(false).withPanoramaExterior('next', panoramaExteriorMock.withFailed().value()).value();
        const wrapper = shallow(<CardImageGallery offer={ card }/>, { context: context }).dive();
        const gallery = wrapper.find('Connect(ImageGalleryDesktop)').dive().dive();
        const panoramaErrorElem = gallery.find('PanoramaProcessingError');

        expect(panoramaErrorElem.isEmptyRender()).toBe(true);
    });

    it('не покажется для валидной панорамы', () => {
        const card = cloneOfferWithHelpers(cardStateMock).withIsOwner(true).withPanoramaExterior().value();
        const wrapper = shallow(<CardImageGallery offer={ card }/>, { context: context }).dive();
        const gallery = wrapper.find('Connect(ImageGalleryDesktop)').dive().dive();
        const panoramaErrorElem = gallery.find('PanoramaProcessingError');

        expect(panoramaErrorElem.isEmptyRender()).toBe(true);
    });
});

describe('панорама:', () => {
    let store;
    let context;

    beforeEach(() => {
        store = mockStore(storeMock);
        context = {
            ...contextMock,
            store,
        };
    });

    it('запоминает состояние вращения внешней панорамы', () => {
        const card = cloneOfferWithHelpers(cardStateMock).withIsOwner(false).withPanoramaExterior().value();
        const wrapper = shallow(<CardImageGallery offer={ card }/>, { context: context }).dive();

        expect(wrapper.find('Connect(ImageGalleryDesktop)').prop('isPanoramaDragging')).toBe(false);

        const imageGallery = wrapper.find('Connect(ImageGalleryDesktop)').dive().dive();
        const panoramaExterior = imageGallery.find('Connect(PanoramaExterior)');
        panoramaExterior.simulate('dragStateChange', true);

        expect(wrapper.find('Connect(ImageGalleryDesktop)').prop('isPanoramaDragging')).toBe(true);
    });

    it('запоминает состояние вращения внутренней панорамы', () => {
        const card = cloneOfferWithHelpers(cardStateMock).withIsOwner(false).withPanoramaInterior().value();
        const wrapper = shallow(<CardImageGallery offer={ card }/>, { context: context }).dive();

        expect(wrapper.find('Connect(ImageGalleryDesktop)').prop('isPanoramaDragging')).toBe(false);

        const imageGallery = wrapper.find('Connect(ImageGalleryDesktop)').dive().dive();
        const panoramaInterior = imageGallery.find('Connect(PanoramaInterior)');
        panoramaInterior.simulate('dragStateChange', true);

        expect(wrapper.find('Connect(ImageGalleryDesktop)').prop('isPanoramaDragging')).toBe(true);
    });
});

describe('BestPriceBlock', () => {
    it('рисует блоки, если оффер новый, есть контекст gallery и заявка еще не была отправлена', () => {
        const store = mockStore(storeMock);
        const context = {
            ...contextMock,
            store,
        };
        const card = cloneOfferWithHelpers(newCardStateMock).withMatchApplicationContexts([ 'gallery' ]).value();

        const wrapper = shallow(<CardImageGallery offer={ card }/>, { context }).dive();
        const imageGalleryDesktop = wrapper.find('Connect(ImageGalleryDesktop)');
        const footer = imageGalleryDesktop.props().renderFooter();
        const sidebar = imageGalleryDesktop.props().renderSidebar();

        expect(footer.type.displayName).toBe('Connect(GalleryBestPriceFooter)');
        expect(sidebar.props.shouldShowBestPriceBlock).toBe(true);
    });

    it('не рисует блоки, если оффер новый, есть контекст gallery, но заявка уже была отправлена', () => {
        const store = mockStore(storeMock);
        const context = {
            ...contextMock,
            store,
        };
        const card = cloneOfferWithHelpers(newCardStateMock).withMatchApplicationContexts([ 'gallery' ]).value();

        const wrapper = shallow(<CardImageGallery offer={ card }/>, { context }).dive();
        const imageGalleryDesktop = wrapper.find('Connect(ImageGalleryDesktop)');
        let footer = imageGalleryDesktop.props().renderFooter();
        footer.props.afterSubmit();

        footer = wrapper.find('Connect(ImageGalleryDesktop)').props().renderFooter();
        const sidebar = imageGalleryDesktop.props().renderSidebar();

        expect(footer.type.displayName).not.toBe('Connect(GalleryBestPriceFooter)');
        expect(sidebar.props.shouldShowBestPriceBlock).toBe(false);
    });

    it('не рисует блоки, если оффер новый, заявка отправлена не была, но контекста нет', () => {
        const store = mockStore(storeMock);
        const context = {
            ...contextMock,
            store,
        };

        const wrapper = shallow(<CardImageGallery offer={ newCardStateMock }/>, { context }).dive();
        const imageGalleryDesktop = wrapper.find('Connect(ImageGalleryDesktop)');
        const footer = imageGalleryDesktop.props().renderFooter();
        const sidebar = imageGalleryDesktop.props().renderSidebar();

        expect(footer.type.displayName).not.toBe('Connect(GalleryBestPriceFooter)');
        expect(sidebar.props.shouldShowBestPriceBlock).toBe(false);
    });

    it('не рисует блоки, если есть контекст gallery и заявка еще не была отправлена, но оффер - б/у', () => {
        const store = mockStore(storeMock);
        const context = {
            ...contextMock,
            store,
        };
        const card = cloneOfferWithHelpers(cardStateMock).withMatchApplicationContexts([ 'gallery' ]).value();

        const wrapper = shallow(<CardImageGallery offer={ card }/>, { context }).dive();
        const imageGalleryDesktop = wrapper.find('Connect(ImageGalleryDesktop)');
        const footer = imageGalleryDesktop.props().renderFooter();
        const sidebar = imageGalleryDesktop.props().renderSidebar();

        expect(footer.type.displayName).not.toBe('Connect(GalleryBestPriceFooter)');
        expect(sidebar.props.shouldShowBestPriceBlock).toBe(false);
    });
});

describe('ссылки на загрузку фоток', () => {
    it('правильно формирует ссылку в QR и в ссылку', () => {
        const store = mockStore(storeMock);
        const context = {
            ...contextMock,
            store,
        };
        const card = cloneOfferWithHelpers(cardStateMock).withPhotoAddUrl('test_url').withImages([]).value();

        const wrapper = shallow(<CardImageGallery offer={ card }/>, { context }).dive();
        const qr = wrapper.find('QRCode');
        const link = wrapper.find('Link');

        expect(qr).toHaveProp('value', 'test_url');
        expect(link).toHaveProp('url', 'link/form/?category=cars&section=used&form_type=edit&sale_id=1085562758&sale_hash=1970f439#photo');
    });
});

it('не должен рисовать бейджи если тачка не активна', () => {
    const store = mockStore(storeMock);
    const context = {
        ...contextMock,
        store,
    };
    const card = cloneOfferWithHelpers(newCardStateMock).withStatus(OfferStatus.INACTIVE).value();

    const wrapper = shallow(<CardImageGallery offer={ card }/>, { context }).dive();

    expect(wrapper.find('.CardImageGallery__badges')).toEqual({});
});
