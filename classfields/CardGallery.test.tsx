/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import 'jest-enzyme';
import React from 'react';
import { shallow } from 'enzyme';

import type { Mark, Model, SuperGeneration } from '@vertis/schema-registry/ts-types-snake/auto/api/catalog_model';
import type { State } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';
import type { Photo, Video } from '@vertis/schema-registry/ts-types-snake/auto/api/common_model';
import { OfferStatus, SellerType } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';

import type { ThunkMockStore } from 'autoru-frontend/mocks/mockStore';
import mockStore from 'autoru-frontend/mocks/mockStore';
import type { TOfferMock } from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import type { StateConfig } from 'auto-core/react/dataDomain/config/StateConfig';
import configStateMock from 'auto-core/react/dataDomain/config/mock';

import CardGallery from './CardGallery';

interface ReduxState {
    config: StateConfig;
}

let offer: TOfferMock;
let store: ThunkMockStore<ReduxState>;
beforeEach(() => {
    offer = cloneOfferWithHelpers({
        category: 'cars',
        section: 'used',
        seller_type: SellerType.PRIVATE,
        state: {
            image_urls: [
                { sizes: { '1200x900n': 'img1' } } as Partial<Photo> as Photo,
                { sizes: { '1200x900n': 'img2' } } as Partial<Photo> as Photo,
                { sizes: { '1200x900n': 'img3' } } as Partial<Photo> as Photo,
            ],
            panoramas: {
                spincar_exterior_url: 'spincar_exterior_url',
            },
            state_not_beaten: true,
            video: { youtube_id: 'ytvideo' } as Partial<Video> as Video,
        } as Partial<State> as State,
        status: OfferStatus.ACTIVE,
        vehicle_info: {
            mark_info: {
                code: 'MERCEDES',
                name: 'Mercedes-Benz',
            } as Mark,
            model_info: {
                code: 'E_KLASSE',
                name: 'S-Класс',
            } as Model,
            super_gen: {
                id: '4593251',
                name: 'IV (W212, S212, C207)',
            } as SuperGeneration,
        },
    }).withYear(2020);

    store = mockStore({
        config: configStateMock.value(),
    });
});

it('должен отрендерить фотки и другие элементы в галереи в правильном порядке', () => {
    const tree = shallow(
        <CardGallery offer={ offer.value() }/>,
        { context: { store } },
    ).dive();
    tree.setState({ visible: true });

    const galleryList = tree.find('ImageFullscreenGallery').dive().find('.ImageFullscreenGallery__list');

    expect(galleryList).toMatchSnapshot();
});

it('не должен отрендерить рекламы для объявления дилера', () => {
    offer = offer.withSellerTypeCommercial();
    const tree = shallow(
        <CardGallery offer={ offer.value() }/>,
        { context: { store } },
    ).dive();
    tree.setState({ visible: true });

    const galleryList = tree.find('ImageFullscreenGallery').dive().find('.ImageFullscreenGallery__list');

    expect(galleryList.find('CardGallery__ad')).toHaveLength(0);
    expect(galleryList.find('Connect(Ad)')).toHaveLength(0);
});

describe('нижний тулбар', () => {
    beforeEach(() => {
        offer = offer
            .withStatus(OfferStatus.ACTIVE)
            .withIsOwner(false);
    });

    describe('активное объявление', () => {
        it('должен отрендерить цену', () => {
            const tree = shallow(
                <CardGallery offer={ offer.value() }/>,
                { context: { store } },
            ).dive();
            tree.setState({ visible: true });

            expect(
                tree.find('ImageFullscreenGallery').dive().find('OfferPriceCaption'),
            ).toHaveProp('offer', offer.value());
        });

        it('должен отрендерить название и год', () => {
            const tree = shallow(
                <CardGallery offer={ offer.value() }/>,
                { context: { store } },
            ).dive();
            tree.setState({ visible: true });

            expect(
                tree.find('ImageFullscreenGallery').dive().find('.CardGallery__bottomTitle'),
            ).toHaveText('Mercedes-Benz S-Класс IV (W212, S212, C207), 2020');
        });

        it('должен отрендерить кнопку позвонить', () => {
            const tree = shallow(
                <CardGallery offer={ offer.value() }/>,
                { context: { store } },
            ).dive();
            tree.setState({ visible: true });

            expect(
                tree.find('ImageFullscreenGallery').dive().find('CardPhoneButton'),
            ).toHaveProp({
                metrikaSource: 'gallery',
                offer: offer.value(),
            });
        });

        it('должен отрендерить кнопку написать, если нельзя позвонить', () => {
            offer = offer.withChatOnly();

            const tree = shallow(
                <CardGallery offer={ offer.value() }/>,
                { context: { store } },
            ).dive();
            tree.setState({ visible: true });

            expect(
                tree.find('ImageFullscreenGallery').dive().find('Connect(OpenChatByOffer)'),
            ).toHaveProp('offer', offer.value());
        });
    });

    describe('неактивное объявление', () => {
        beforeEach(() => {
            offer = offer
                .withStatus(OfferStatus.INACTIVE);
        });

        it('должен отрендерить название и год без кнопки позвонить', () => {
            const tree = shallow(
                <CardGallery offer={ offer.value() }/>,
                { context: { store } },
            ).dive();
            tree.setState({ visible: true });

            expect(
                tree.find('ImageFullscreenGallery').dive().find('.CardGallery__bottom'),
            ).toMatchSnapshot();
        });
    });

    describe('для владельца', () => {
        beforeEach(() => {
            offer = offer
                .withIsOwner(true);
        });

        it('должен отрендерить название и год без кнопки позвонить', () => {
            const tree = shallow(
                <CardGallery offer={ offer.value() }/>,
                { context: { store } },
            ).dive();
            tree.setState({ visible: true });

            expect(
                tree.find('ImageFullscreenGallery').dive().find('.CardGallery__bottom'),
            ).toMatchSnapshot();
        });
    });

    describe('Блок Получить лучшую цену', () => {
        it('должен рендерить если прокинут проп shouldShowBestPriceBlock', () => {
            const tree = shallow(
                <CardGallery offer={ offer.value() } shouldShowBestPriceBlock/>,
                { context: { store } },
            ).dive();
            tree.setState({ visible: true });

            expect(
                tree.find('ImageFullscreenGallery').dive().find('GalleryBestPriceMobile').exists(),
            ).toBe(true);
        });

        it('не должен рендерить если пропа shouldShowBestPriceBlock нет', () => {
            const tree = shallow(
                <CardGallery offer={ offer.value() }/>,
                { context: { store } },
            ).dive();
            tree.setState({ visible: true });

            expect(
                tree.find('ImageFullscreenGallery').dive().find('GalleryBestPriceMobile').exists(),
            ).toBe(false);
        });
    });
});
