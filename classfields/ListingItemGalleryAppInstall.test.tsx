import React from 'react';
import { shallow } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';

import ListingItemGalleryAppInstall from './ListingItemGalleryAppInstall';

it(`должен отрендерить карточку-эксперимент - Скачать`, async() => {
    const itemsCount = 10;
    const appBadgeType = 'download';
    const url = 'cars/used/sale/bmw/m5/1106241485-f6066acd/';
    const from = 'direct';

    const page = shallow(
        <ListingItemGalleryAppInstall
            itemsCount={ itemsCount }
            appBadgeType={ appBadgeType }
            url={ url }
            from={ from }/>,
        { context: { hasExperiment: () => false } },
    );

    const buttonUrl = page.find('.ListingItemGalleryAppInstall__button');

    expect(buttonUrl.props()).toHaveProperty('url',
        // eslint-disable-next-line max-len
        'https://sb76.adj.st/cars/used/sale/bmw/m5/1106241485-f6066acd/?adjust_deeplink=autoru%3A%2F%2Fapp%2Fcars%2Fused%2Fsale%2Fbmw%2Fm5%2F1106241485-f6066acd%2F&adjust_t=m1nelw7_eb04l75&adjust_campaign=touch_photo_cards&adjust_adgroup=download&adjust_creative=direct&adjust_fallback=https%3A%2F%2Fautoru_frontend.base_domain%2Fcars%2Fused%2Fsale%2Fbmw%2Fm5%2F1106241485-f6066acd%2F');
});

it(`должен отрендерить карточку-эксперимент - Смотреть`, async() => {
    const itemsCount = 10;
    const appBadgeType = 'see';
    const url = 'cars/used/sale/bmw/m5/1106241485-f6066acd/';
    const from = 'direct';

    const page = shallow(
        <ListingItemGalleryAppInstall
            itemsCount={ itemsCount }
            appBadgeType={ appBadgeType }
            url={ url }
            from={ from }/>,
        { context: { hasExperiment: () => false } },
    );

    const buttonUrl = page.find('.ListingItemGalleryAppInstall__button');

    expect(buttonUrl.props()).toHaveProperty('url',
        // eslint-disable-next-line max-len
        'https://sb76.adj.st/cars/used/sale/bmw/m5/1106241485-f6066acd/?adjust_deeplink=autoru%3A%2F%2Fapp%2Fcars%2Fused%2Fsale%2Fbmw%2Fm5%2F1106241485-f6066acd%2F&adjust_t=m1nelw7_eb04l75&adjust_campaign=touch_photo_cards&adjust_adgroup=see&adjust_creative=direct&adjust_fallback=https%3A%2F%2Fautoru_frontend.base_domain%2Fcars%2Fused%2Fsale%2Fbmw%2Fm5%2F1106241485-f6066acd%2F');
});

it(`должен отрендерить карточку-эксперимент - 1 день 1 карточка`, async() => {
    const itemsCount = 10;
    const appBadgeType = 'download';
    const url = 'cars/used/sale/bmw/m5/1106241485-f6066acd/';
    const from = 'direct';

    contextMock.hasExperiment.mockImplementation((exp) => exp === 'AUTORUFRONT-21187_offer_gallery_badge_first_entry');

    const page = shallow(
        <ListingItemGalleryAppInstall
            itemsCount={ itemsCount }
            appBadgeType={ appBadgeType }
            url={ url }
            from={ from }/>,
        { context: contextMock },
    );

    const buttonUrl = page.find('.ListingItemGalleryAppInstall__button');

    expect(buttonUrl.props()).toHaveProperty('url',
        // eslint-disable-next-line max-len
        'https://sb76.adj.st/cars/used/sale/bmw/m5/1106241485-f6066acd/?adjust_deeplink=autoru%3A%2F%2Fapp%2Fcars%2Fused%2Fsale%2Fbmw%2Fm5%2F1106241485-f6066acd%2F&adjust_t=m1nelw7_eb04l75&adjust_campaign=touch_photo_cards&adjust_adgroup=1entry_1card_download&adjust_creative=direct&adjust_fallback=https%3A%2F%2Fautoru_frontend.base_domain%2Fcars%2Fused%2Fsale%2Fbmw%2Fm5%2F1106241485-f6066acd%2F');
});

it(`должен отрендерить карточку-эксперимент - 1 день 4 карточка`, async() => {
    const itemsCount = 10;
    const appBadgeType = 'download';
    const url = 'cars/used/sale/bmw/m5/1106241485-f6066acd/';
    const from = 'direct';

    contextMock.hasExperiment.mockImplementation((exp) => exp === 'AUTORUFRONT-21187_offer_gallery_badge_fourth_card');

    const page = shallow(
        <ListingItemGalleryAppInstall
            itemsCount={ itemsCount }
            appBadgeType={ appBadgeType }
            url={ url }
            from={ from }/>,
        { context: contextMock },
    );

    const buttonUrl = page.find('.ListingItemGalleryAppInstall__button');

    expect(buttonUrl.props()).toHaveProperty('url',
        // eslint-disable-next-line max-len
        'https://sb76.adj.st/cars/used/sale/bmw/m5/1106241485-f6066acd/?adjust_deeplink=autoru%3A%2F%2Fapp%2Fcars%2Fused%2Fsale%2Fbmw%2Fm5%2F1106241485-f6066acd%2F&adjust_t=m1nelw7_eb04l75&adjust_campaign=touch_photo_cards&adjust_adgroup=1entry_4card_download&adjust_creative=direct&adjust_fallback=https%3A%2F%2Fautoru_frontend.base_domain%2Fcars%2Fused%2Fsale%2Fbmw%2Fm5%2F1106241485-f6066acd%2F');
});

it(`должен отрендерить карточку-эксперимент - 2 день 1 карточка`, async() => {
    const itemsCount = 10;
    const appBadgeType = 'download';
    const url = 'cars/used/sale/bmw/m5/1106241485-f6066acd/';
    const from = 'direct';

    contextMock.hasExperiment.mockImplementation((exp) => exp === 'AUTORUFRONT-21187_offer_gallery_badge_second_entry');

    const page = shallow(
        <ListingItemGalleryAppInstall
            itemsCount={ itemsCount }
            appBadgeType={ appBadgeType }
            url={ url }
            from={ from }/>,
        { context: contextMock },
    );

    const buttonUrl = page.find('.ListingItemGalleryAppInstall__button');

    expect(buttonUrl.props()).toHaveProperty('url',
        // eslint-disable-next-line max-len
        'https://sb76.adj.st/cars/used/sale/bmw/m5/1106241485-f6066acd/?adjust_deeplink=autoru%3A%2F%2Fapp%2Fcars%2Fused%2Fsale%2Fbmw%2Fm5%2F1106241485-f6066acd%2F&adjust_t=m1nelw7_eb04l75&adjust_campaign=touch_photo_cards&adjust_adgroup=2entry_1card_download&adjust_creative=direct&adjust_fallback=https%3A%2F%2Fautoru_frontend.base_domain%2Fcars%2Fused%2Fsale%2Fbmw%2Fm5%2F1106241485-f6066acd%2F');
});
