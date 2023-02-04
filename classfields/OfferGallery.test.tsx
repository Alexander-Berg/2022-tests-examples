import React from 'react';
import _ from 'lodash';
import type { ShallowWrapper } from 'enzyme';
import { shallow } from 'enzyme';

import { OfferStatus } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';

import groupCardMock from 'autoru-frontend/mockData/state/groupCard.mock';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';
import contextMock from 'autoru-frontend/mocks/contextMock';

import cardStateMock from 'auto-core/react/dataDomain/card/mocks/card.cars.mock';

import type { Props, State } from './OfferGallery';
import OfferGallery from './OfferGallery';

let props: Props;
beforeEach(() => {
    props = {
        onClick: jest.fn(),
        url: 'offer-url',
        offer: cardStateMock,
    };
});

describe('если у оффера есть панорама', () => {
    let page: ShallowWrapper;
    beforeEach(() => {
        page = shallowRenderComponent({
            props: {
                ...props,
                offer: cloneOfferWithHelpers(cardStateMock).withPanoramaExterior().value(),
                customBadge: (
                    <div className="customBadge">кастомный бэйдж</div>
                ),
            },
        });
    });

    it('добавит на первое место превью панорамы', () => {
        const panorama = page.find('.OfferGallery__item').at(0).find('OfferPanorama');
        expect(panorama.isEmptyRender()).toBe(false);
    });

    it('добавит в конец кастомный бэйдж', () => {
        const panorama = page.find('.OfferGallery__item').at(0).find('.customBadge');
        expect(panorama).toHaveLength(1);
    });
});

describe('у обычного оффера', () => {
    let page: ShallowWrapper;
    beforeEach(() => {
        page = shallowRenderComponent({
            props: {
                ...props,
                customBadge: (
                    <div className="customBadge">кастомный бэйдж</div>
                ),
            },
        });
    });

    it('у первого элемента в галереи нет иконки 360', () => {
        const firstItemIcon = page.find('.OfferGallery__item').at(0).find('IconSvg');
        expect(firstItemIcon.isEmptyRender()).toBe(true);
    });

    it('добавит в конец кастомный бэйдж', () => {
        const panorama = page.find('.OfferGallery__item').at(0).find('.customBadge');
        expect(panorama).toHaveLength(1);
    });
});

it('для группового оффера правильно возьмет фото из vendor_colors, а не из объявления', () => {
    const page = shallowRenderComponent({
        props: {
            ...props,
            offer: groupCardMock,
        },
    });

    expect((page.state() as State).items[0]).toEqual({
        alt: '2019 BMW 3 серия 320i xDrive VII (G2x), белый, 2940000 рублей, вид 1',
        name: '34-front',
        sizes: {
            orig: 'picture-of-cat',
            wizardv3mr: 'picture-of-cat',
            cattouch: 'picture-of-cat',
        },
        type: 'IMAGE',
    });
});

it('нет бейджа Продан у активного объявления', () => {
    const page = shallowRenderComponent({
        props: {
            ...props,
            showSoldBadge: true,
        },
    });
    const soldBadge = page.find('.OfferGallery__itemBadgeSold');
    expect(soldBadge.isEmptyRender()).toBe(true);
});

it('не должен рендерить бейджи у не активного объявления', () => {
    const newProps = _.cloneDeep(props);
    newProps.offer.status = OfferStatus.INACTIVE;
    const page = shallowRenderComponent({ props: newProps });

    expect(page.find('.OfferGallery__itemTopBadges').children()).toHaveLength(0);
    expect(page.find('.OfferGallery__itemBottomBadges').children()).toHaveLength(0);
});

function shallowRenderComponent({ props }: { props: Props }) {
    return shallow(<OfferGallery { ...props }/>, { context: contextMock });
}
