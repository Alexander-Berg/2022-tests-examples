import 'jest-enzyme';
import React from 'react';
import { shallow } from 'enzyme';

import { Multiposting_Classified_ClassifiedName as ClassifiedName, OfferStatus } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';

import type { MultipostingClassifiedAvito } from 'auto-core/types/Multiposting';
import OFFER_STATUSES from 'auto-core/data/offer-statuses.json';

import OfferSnippetRoyalErrors from './OfferSnippetRoyalErrors';

const store = mockStore({});
const BAN_REASONS = [
    {
        title: 'первая причина это ты',
        text_lk_dealer: '',
        text: '',
        text_app: '',
        text_app_html: '',
        text_lk_dealer_app: '',
    },
    {
        title: 'а вторая все твои мечты',
        text_lk_dealer: '',
        text: '',
        text_app: '',
        text_app_html: '',
        text_lk_dealer_app: '',
    },
];

const offerObj = cloneOfferWithHelpers(offerMock).withStatus(OFFER_STATUSES.banned as OfferStatus);

const defaultProps = {
    canWriteSaleResource: true,
    isMultipostingEnabled: true,
    offer: offerObj.value(),
};

describe('баны', () => {
    it('отобразит причину бана, если она одна', () => {
        const reason = BAN_REASONS[0];
        const tree = shallowRenderComponent({
            ...defaultProps,
            offer: offerObj.withHumanReasonsBan([ reason ]).value(),
        });

        expect(tree.find('.OfferSnippetRoyalErrors__reason').text()).toEqual(reason.title);
    });

    it('покажет, что причин бана несколько', () => {
        const tree = shallowRenderComponent({
            ...defaultProps,
            offer: offerObj.withHumanReasonsBan(BAN_REASONS).value(),
        });

        expect(tree.find('.OfferSnippetRoyalErrors__reason').text()).toEqual('нескольким причинам');
    });

    it('покажет текст, что объявление заблокировано, если нет причин бана', () => {
        const tree = shallowRenderComponent({
            ...defaultProps,
            offer: offerObj.value(),
        });

        expect(tree).toIncludeText('Объявление заблокировано');
    });
});

it('покажет кнопку "активироать", если объявление просрочено и есть права', () => {
    const tree = shallowRenderComponent({
        ...defaultProps,
        offer: offerObj
            .withStatus(OFFER_STATUSES.expired as OfferStatus)
            .withAction({ activate: true, edit: true, archive: true, hide: true })
            .value(),
    });

    expect(tree.find('Button').dive().text()).toEqual('Активировать');
});

it('отобразит бан на Авито', () => {
    const tree = shallowRenderComponent({
        ...defaultProps,
        offer: offerObj
            .withStatus(OFFER_STATUSES.active as OfferStatus)
            .withMultiposting({
                status: OfferStatus.ACTIVE,
                actions: { edit: true, activate: false, hide: true, archive: true },
                classifieds: [
                    {
                        name: ClassifiedName.AVITO,
                        status: OfferStatus.BANNED,
                        enabled: true,
                    } as unknown as MultipostingClassifiedAvito,
                ],
            })
            .value(),
    });

    expect(tree.find('HoveredTooltip').dive()).toIncludeText('Объявление заблокировано на Авито');
});

function shallowRenderComponent(props = defaultProps) {
    return shallow(
        <OfferSnippetRoyalErrors { ...props }/>,
        { context: { ...contextMock, store } },
    ).dive();
}
