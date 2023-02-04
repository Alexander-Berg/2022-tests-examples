/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import type { ReactElement } from 'react';
import React from 'react';
import { shallow } from 'enzyme';
import { cloneDeep } from 'lodash';

import { SocialProvider } from '@vertis/schema-registry/ts-types-snake/vertis/common';
import type { AdditionalInfo } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';
import { Car_EngineType } from '@vertis/schema-registry/ts-types-snake/auto/api/cars_model';

import { getBunkerMock } from 'autoru-frontend/mockData/state/bunker.mock';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';
import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import { nbsp } from 'auto-core/react/lib/html-entities';
import userMock from 'auto-core/react/dataDomain/user/mocks';

import { BenefitTag } from 'auto-core/types/TBenefitTag';
import type { Offer, TOfferVehicleInfo } from 'auto-core/types/proto/auto/api/api_offer_model';

import CardBenefits from './CardBenefitsDesktop';

let offer: Offer;
let state: any;
beforeEach(() => {
    state = {
        config: {
            data: {},
        },
        bunker: getBunkerMock([ 'moderation/proven_owner', 'desktop/state_support' ]),
        user: { data: {} },
    };

    offer = {
        category: 'cars',
        tags: [ 'proven_owner', 'no_accidents', 'one_owner', 'warranty', 'almost_new', 'safe_car', 'high_reviews_mark', 'stable_price', 'autoru_exclusive' ],
        documents: {
            warranty: true,
            warranty_expire: {
                year: 2022,
                month: 8,
                day: 1,
            },
            purchase_date: {
                year: 2018,
                month: 12,
            },
        },
        price_info: {},
        vehicle_info: {
            configuration: { body_type_group: 'HATCHBACK_5_DOORS' },
            tech_param: {},
        } as Partial<TOfferVehicleInfo> as TOfferVehicleInfo,
        additional_info: {
            review_summary: {
                counter: 100,
                avg_rating: 4,
            },
            price_stats: { last_year_price_percentage_diff: -3 },
        } as Partial<AdditionalInfo> as AdditionalInfo,
    } as Partial<Offer> as Offer;
});

it('Должен быть пункт про ДТП', () => {
    const context = cloneDeep(contextMock);

    const offerWithDtp = cloneOfferWithHelpers(offer).withTags([ 'autoru_exclusive', 'no_accidents', 'warranty' ]);

    const wrapper = shallow(
        <CardBenefits offer={ offerWithDtp.value() }/>, { context: { ...context, store: mockStore(state) } },
    );

    const titles = wrapper.dive().dive().dive().find('.CardBenefits__item-title');
    const dtpItem = titles.findWhere(node => node.text() === `ДТП не${ nbsp }найдены`);

    expect(dtpItem.isEmptyRender()).toBe(false);
});

it('Должен быть пункт про онлайн-показ', () => {
    const context = cloneDeep(contextMock);
    const offerWithOnline = cloneOfferWithHelpers(offer)
        .withTags([ 'online_view_available', 'proven_owner' ]);

    const wrapper = shallow(
        <CardBenefits offer={ offerWithOnline.value() }/>, { context: { ...context, store: mockStore(state) } },
    );

    const titles = wrapper.dive().dive().find('.CardBenefits__item-title');
    const dtpItem = titles.findWhere(node => node.text() === `Онлайн-показ`);

    expect(dtpItem.isEmptyRender()).toBe(false);
});

it('Должен вывести правильный текст в попапе для онлайн-показа оффера нового авто', () => {
    const context = cloneDeep(contextMock);

    const wrapper = shallow(
        <CardBenefits

            offer={{ ...offer, section: 'new', tags: [ 'online_view_available' ] }}
        />, { context: { ...context, store: mockStore(state) } },
    );

    const popupContent = wrapper.dive().dive().find('InfoPopup').prop('content');

    expect(popupContent).toMatchSnapshot();
});

it('Должен вывести попап об оценке объявления', () => {
    const context = cloneDeep(contextMock);

    const offerWithScore = cloneOfferWithHelpers(offer).withTags([]).withScore();

    const wrapper = shallow(
        <CardBenefits offer={ offerWithScore.value() }/>,
        {
            context: {
                ...context,
                store: mockStore(state),
            },
        },
    );

    const popupContent: unknown = wrapper.dive().dive().find('InfoPopup').prop('content');
    const popupContentChild = (popupContent as ReactElement).props.children;

    expect(shallow(popupContentChild, { context })).toHaveClassName('.CardBenefitsScoreTransparencyPopup');
});

it('Должен быть пункт об оценке объявления', () => {
    const context = cloneDeep(contextMock);

    const offerWithScore = cloneOfferWithHelpers(offer).withTags([]).withScore();

    const wrapper = shallow(
        <CardBenefits offer={ offerWithScore.value() }/>,
        {
            context: {
                ...context,
                store: mockStore(state),
            },
        },
    );

    const titles = wrapper.dive().dive().find('.CardBenefits__item-title');
    const scoreItem = titles.findWhere(node => node.text() === 'Качественное объявление');

    expect(scoreItem.isEmptyRender()).toBe(false);
});

describe('преимущество "Господдержка"', () => {
    let offerWithStateSupport: Offer;
    beforeEach(() => {
        offerWithStateSupport = cloneOfferWithHelpers(offer)
            .withCategory('cars')
            .withSection('new')
            .withMarkInfo({ code: 'RENAULT', name: 'RENAULT' })
            .withModelInfo({ code: 'DUSTER', name: 'DUSTER' })
            .withPrice(1100000)
            .withTags([ 'online_view_available' ])
            .value();
    });

    it('должен отрендерить первым', () => {
        const context = cloneDeep(contextMock);

        const wrapper = shallow(
            <CardBenefits offer={ offerWithStateSupport }/>, { context: { ...context, store: mockStore(state) } },
        ).dive().dive();

        const itemTitles = wrapper.find('.CardBenefits__item-title');

        expect(itemTitles.at(0).text()).toBe('-10% от цены');
        expect(itemTitles.at(1).text()).toBe('Онлайн-показ');
    });
});

it('не должен отрендерить скор менее 7 для не владельца', () => {
    const context = cloneDeep(contextMock);

    const offerWithOwnerAndScore = cloneOfferWithHelpers(offer)
        .withTags([])
        .withScore({ transparency: 60 })
        .value();

    const wrapper = shallow(
        <CardBenefits offer={ offerWithOwnerAndScore }/>, { context: { ...context, store: mockStore(state) } },
    ).dive().dive();

    expect(wrapper.isEmptyRender()).toBe(true);
});

it('должен отрендерить скор более 7 для не владельца', () => {
    const context = cloneDeep(contextMock);

    const offerWithOwnerAndScore = cloneOfferWithHelpers(offer)
        .withTags([])
        .withScore({ transparency: 80 })
        .value();

    const wrapper = shallow(
        <CardBenefits offer={ offerWithOwnerAndScore }/>, {
            context: {
                ...context,
                store: mockStore(state),
            },
        },
    ).dive().dive();

    const itemTitles = wrapper.find('.CardBenefits__item-title');
    expect(itemTitles.at(0).text()).toBe('Качественное объявление');
});

it('должен отрендерить score_transparency_owner, если есть что улучшить', () => {
    const context = cloneDeep(contextMock);
    const stateWithScores = {
        ...state,
        scoreTransparency: { transparency_scoring: { mosru_score: 0 } },
    };

    const offerWithOwnerAndScore = cloneOfferWithHelpers(offer)
        .withTags([])
        .withIsOwner(true)
        .withScore({ transparency: 60 })
        .value();

    const wrapper = shallow(
        <CardBenefits offer={ offerWithOwnerAndScore }/>, {
            context: {
                ...context,
                store: mockStore(stateWithScores),
            },
        },
    ).dive().dive();

    const itemTitles = wrapper.find('.CardBenefits__item-title');
    expect(itemTitles.at(0).text()).toBe('Качество объявления 6/10');
});

it('должен отрендерить score_transparency_owner, а не просто скор больше 7, если есть что улучшить', () => {
    const context = cloneDeep(contextMock);
    const stateWithScores = {
        ...state,
        scoreTransparency: { transparency_scoring: { mosru_score: 0 } },
    };

    const offerWithOwnerAndScore = cloneOfferWithHelpers(offer)
        .withTags([])
        .withIsOwner(true)
        .withScore({ transparency: 80 })
        .value();

    const wrapper = shallow(
        <CardBenefits offer={ offerWithOwnerAndScore }/>, {
            context: {
                ...context,
                store: mockStore(stateWithScores),
            },
        },
    ).dive().dive();

    const itemTitles = wrapper.find('.CardBenefits__item-title');
    expect(itemTitles.at(0).text()).toBe('Качество объявления 8/10');
});

it('не должен отрендерить score_transparency_owner, если нечего улучшить', () => {
    const context = cloneDeep(contextMock);

    const offerWithOwnerAndScore = cloneOfferWithHelpers(offer)
        .withTags([ ])
        .withIsOwner(true)
        .withScore({ transparency: 60 })
        .value();

    state.user = userMock.withAuth(true).withSocialProfiles([ {
        provider: SocialProvider.GOSUSLUGI,
        social_user_id: '1',
        nickname: 'jd',
        first_name: 'john',
        last_name: 'doe',
        trusted: true,
    } ]).value();

    const wrapper = shallow(
        <CardBenefits offer={ offerWithOwnerAndScore }/>, {
            context: {
                ...context,
                store: mockStore(state),
            },
        },
    ).dive().dive();

    const itemTitles = wrapper.find('.CardBenefits__item-title');
    expect(itemTitles).toHaveLength(1);
    expect(itemTitles.at(0).text()).toBe('Госуслуги');
});

it('Не должно быть дилерских преимуществ для частника', () => {
    const context = cloneDeep(contextMock);
    const testOffer = cloneOfferWithHelpers(offer)
        .withTags([ 'chats_enabled' ])
        .withSellerTypePrivate();

    const wrapper = shallow(
        <CardBenefits offer={ testOffer.value() }/>,
        {
            context: {
                ...context,
                store: mockStore(state),
            },
        },
    );

    const titles = wrapper.dive().dive().find('.CardBenefits__item-title');
    const dealerItem = titles.findWhere(node => node.text() === 'Дилер всегда на связи');

    expect(dealerItem.isEmptyRender()).toBe(true);
});

it('Должны быть дилерские преимущества для дилера', () => {
    const context = cloneDeep(contextMock);
    const testOffer = cloneOfferWithHelpers(offer)
        .withTags([ 'chats_enabled' ])
        .withSellerTypeCommercial();

    const wrapper = shallow(
        <CardBenefits offer={ testOffer.value() }/>,
        {
            context: {
                ...context,
                store: mockStore(state),
            },
        },
    );

    const titles = wrapper.dive().dive().find('.CardBenefits__item-title');

    expect(titles.text()).toBe('Дилер всегда на связи');
});

it('Должен не добавлять модификатор withPopup, если попапа нет', () => {
    const context = cloneDeep(contextMock);
    const testOffer = cloneOfferWithHelpers(offer)
        .withTags([ 'chats_enabled' ])
        .withSellerTypeCommercial();

    const wrapper = shallow(
        <CardBenefits offer={ testOffer.value() }/>,
        {
            context: {
                ...context,
                store: mockStore(state),
            },
        },
    );

    const item = wrapper.dive().dive().find('.CardBenefits__item').get(0);

    expect(item.props['className']).toBe('CardBenefits__item CardBenefits__item_dealerBenefit');
});

it('Должен добавить модификатор withPopup, если попап есть', () => {
    const context = cloneDeep(contextMock);
    const testOffer = cloneOfferWithHelpers(offer)
        .withTags([ 'chats_enabled', 'near_to_you' ])
        .withSellerTypeCommercial();

    const wrapper = shallow(
        <CardBenefits offer={ testOffer.value() }/>,
        {
            context: {
                ...context,
                store: mockStore(state),
            },
        },
    );

    const item = wrapper.dive().dive().find('.CardBenefits__item').get(0);

    expect(item.props['className']).toBe('CardBenefits__item CardBenefits__item_withPopup CardBenefits__item_dealerBenefit');
});

it('Должен быть пункт только на Авто.ру, если isBooked=true', () => {
    const context = cloneDeep(contextMock);

    const testOffer = cloneOfferWithHelpers(offer)
        .withTags([ 'autoru_exclusive', 'near_to_you' ])
        .withBooking({ allowed: false });

    const wrapper = shallow(
        <CardBenefits offer={ testOffer.value() }/>, { context: { ...context, store: mockStore(state) } },
    );

    const titles = wrapper.dive().dive().find('.CardBenefits__item-title');
    const itemTitle = titles.findWhere(node => node.text() === `Только на${ nbsp }Авто.ру`);

    expect(itemTitle.isEmptyRender()).toBe(false);
});

it('не должно быть пункта только на Авто.ру, если !isBooked', () => {
    const context = cloneDeep(contextMock);

    const wrapper = shallow(
        <CardBenefits offer={ offer }/>, { context: { ...context, store: mockStore(state) } },
    );

    const titles = wrapper.dive().dive().find('.CardBenefits__item-title');
    const itemTitle = titles.findWhere(node => node.text() === `Только на${ nbsp }Авто.ру`);

    expect(itemTitle.isEmptyRender()).toBe(true);
});

describe('преимущества госуслуг', () => {
    describe('метрика на показ блока', () => {
        it('для покупателя', () => {
            const context = cloneDeep(contextMock);

            const testOffer = cloneOfferWithHelpers(offer)
                .withTags([ BenefitTag.GOS_USLUGI ])
                .withBooking();

            shallow(
                <CardBenefits offer={ testOffer.value() }/>, { context: { ...context, store: mockStore(state) } },
            ).dive().dive();

            expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
            expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'advantages', 'show', 'gosuslugi_linked' ]);
        });

        it('для продавца без привязанного акка', () => {
            const context = cloneDeep(contextMock);

            const testOffer = cloneOfferWithHelpers(offer)
                .withTags([ ])
                .withBooking()
                .withIsOwner(true);

            state.user = userMock.withAuth(true).withSocialProfiles([]).value();

            shallow(
                <CardBenefits offer={ testOffer.value() }/>, { context: { ...context, store: mockStore(state) } },
            ).dive().dive();

            expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
            expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'advantages', 'show-request', 'add_gosuslugi' ]);
        });

        it('для продавца с привязанным акком', () => {
            const context = cloneDeep(contextMock);

            const testOffer = cloneOfferWithHelpers(offer)
                .withTags([ ])
                .withBooking()
                .withIsOwner(true);

            state.user = userMock.withAuth(true).withSocialProfiles([ {
                provider: SocialProvider.GOSUSLUGI,
                social_user_id: '1',
                nickname: 'jd',
                first_name: 'john',
                last_name: 'doe',
                trusted: false,
            } ]).value();

            shallow(
                <CardBenefits offer={ testOffer.value() }/>, { context: { ...context, store: mockStore(state) } },
            ).dive().dive();

            expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
            expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'advantages', 'show-request', 'verify_gosuslugi' ]);
        });
    });

    it('должно быть для продавца', () => {
        const context = cloneDeep(contextMock);

        const testOffer = cloneOfferWithHelpers(offer)
            .withTags([])
            .withBooking()
            .withIsOwner(true);

        const page = shallow(
            <CardBenefits offer={ testOffer.value() }/>, { context: { ...context, store: mockStore(state) } },
        ).dive().dive();

        const link = page.find('.CardBenefits__item-link');
        expect(link.children().text()).toBe('Привязать');
    });

    it('не должно быть для продавца, если он дилер', () => {
        const context = cloneDeep(contextMock);

        const testOffer = cloneOfferWithHelpers(offer)
            .withTags([])
            .withBooking()
            .withDealerSeller()
            .withIsOwner(true);

        const page = shallow(
            <CardBenefits offer={ testOffer.value() }/>, { context: { ...context, store: mockStore(state) } },
        ).dive().dive();

        expect(page.isEmptyRender()).toBe(true);

    });

    describe('метрика на показ попапа', () => {
        it('для покупателя', () => {
            const context = cloneDeep(contextMock);

            const testOffer = cloneOfferWithHelpers(offer)
                .withTags([ BenefitTag.GOS_USLUGI ])
                .withBooking();

            const page = shallow(
                <CardBenefits offer={ testOffer.value() }/>, { context: { ...context, store: mockStore(state) } },
            ).dive().dive();

            contextMock.metrika.sendPageEvent.mockClear();

            const popup = page.find('.CardBenefits__item-info-popup');
            popup.simulate('showPopup');

            expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
            expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'advantages', 'show-popup', 'gosuslugi_linked' ]);
        });

        it('для продавца без привязанного акка', () => {
            const context = cloneDeep(contextMock);

            const testOffer = cloneOfferWithHelpers(offer)
                .withTags([])
                .withBooking()
                .withIsOwner(true);

            state.user = userMock.withAuth(true).withSocialProfiles([]).value();

            const page = shallow(
                <CardBenefits offer={ testOffer.value() }/>, { context: { ...context, store: mockStore(state) } },
            ).dive().dive();

            contextMock.metrika.sendPageEvent.mockClear();

            const popup = page.find('.CardBenefits__item-info-popup');
            popup.simulate('showPopup');

            expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
            expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'advantages', 'show-request-popup', 'add_gosuslugi' ]);
        });

        it('для продавца с привязанным акком', () => {
            const context = cloneDeep(contextMock);

            const testOffer = cloneOfferWithHelpers(offer)
                .withTags([])
                .withBooking()
                .withIsOwner(true);

            state.user = userMock.withAuth(true).withSocialProfiles([ {
                provider: SocialProvider.GOSUSLUGI,
                social_user_id: '1',
                nickname: 'jd',
                first_name: 'john',
                last_name: 'doe',
                trusted: false,
            } ]).value();

            const page = shallow(
                <CardBenefits offer={ testOffer.value() }/>, { context: { ...context, store: mockStore(state) } },
            ).dive().dive();

            contextMock.metrika.sendPageEvent.mockClear();

            const popup = page.find('.CardBenefits__item-info-popup');
            popup.simulate('showPopup');

            expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
            expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'advantages', 'show-request-popup', 'verify_gosuslugi' ]);
        });
    });
});

describe('преимущества электро', () => {
    it('отправит метрику при показе тултипа', () => {
        const context = cloneDeep(contextMock);

        const testOffer = cloneOfferWithHelpers(offer)
            .withTags([ BenefitTag.ELECTRO_ENGINE ])
            .withEngineType(Car_EngineType.ELECTRO);

        const wrapper = shallow(
            <CardBenefits offer={ testOffer.value() }/>, { context: { ...context, store: mockStore(state) } },
        ).dive().dive();

        wrapper.find('InfoPopup').simulate('hover');

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'advantages', 'show', 'electro_engine' ]);
    });
});
