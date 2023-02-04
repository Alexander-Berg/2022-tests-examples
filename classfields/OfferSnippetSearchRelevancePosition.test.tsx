import 'jest-enzyme';
import React from 'react';
import { shallow } from 'enzyme';

import { OfferPosition_OrderedPosition_Sort } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';

import contextMock from 'autoru-frontend/mocks/contextMock';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';
import createLinkMock from 'autoru-frontend/mocks/createLinkMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';

import OfferSnippetSearchRelevancePosition from './OfferSnippetSearchRelevancePosition';

const customContext = {
    ...contextMock,
    linkToDesktop: createLinkMock('linkCabinet'),
};

const defaultProps = {
    offer: getOfferWithSearchPosition(),
    canWriteSaleResource: true,
    areServicesDisabled: false,
};

describe('эксперимент 1', () => {
    it('покажет дизайн из эксперимента 1 при соответствующих пропсах', () => {
        const tree = shallowRenderComponent();
        expect(tree.find('.OfferSnippetSearchRelevancePosition__icon')).toExist();
    });

    it('не покажет кнопку "поднять", если у пользователя нет соответствующих прав', () => {
        const tree = shallowRenderComponent({ ...defaultProps, canWriteSaleResource: false });
        expect(tree.find('.OfferSnippetSearchRelevancePosition__tooltipButton')).not.toExist();
    });

    it('не покажет кнопку "поднять", если услуги задизейблены', () => {
        const tree = shallowRenderComponent({ ...defaultProps, areServicesDisabled: true });
        expect(tree.find('.OfferSnippetSearchRelevancePosition__tooltipButton')).not.toExist();
    });

    it('покажет соответствующую иконку, если позиция выше 10', () => {
        const tree = shallowRenderComponent({
            ...defaultProps,
            offer: getOfferWithSearchPosition(1),
        });
        expect(tree.find('.OfferSnippetSearchRelevancePosition__icon_good')).toExist();
    });

    it('покажет соответствующую иконку, если позиция ниже 10, но выше 37 (первая страница выдачи)', () => {
        const tree = shallowRenderComponent({
            ...defaultProps,
            offer: getOfferWithSearchPosition(15),
        });
        expect(tree.find('.OfferSnippetSearchRelevancePosition__icon_bad')).toExist();
    });

    it('покажет соответствующую иконку, если позиция ниже 37', () => {
        const tree = shallowRenderComponent({
            ...defaultProps,
            offer: getOfferWithSearchPosition(40),
        });
        expect(tree.find('.OfferSnippetSearchRelevancePosition__icon_veryBad')).toExist();
    });
});

describe('эксперимент 2', () => {
    const exp2Store = {
        bunker: {
            'cabinet/offer_snippet_relevance': { isExp2: true },
        },
    };

    it('покажет дизайн из эксперимента 2 при соответствующих пропсах', () => {
        const tree = shallowRenderComponent(null, exp2Store);
        expect(tree.find('.OfferSnippetSearchRelevancePosition__tooltipedText')).toExist();
    });

    it('не покажет кнопку "поднять", если у пользователя нет соответствующих прав', () => {
        const tree = shallowRenderComponent({ ...defaultProps, canWriteSaleResource: false }, exp2Store);
        expect(tree.find('.OfferSnippetSearchRelevancePosition__tooltipedText')).not.toExist();
    });

    it('не покажет кнопку "поднять", если услуги задизейблены', () => {
        const tree = shallowRenderComponent({ ...defaultProps, areServicesDisabled: true }, exp2Store);
        expect(tree.find('.OfferSnippetSearchRelevancePosition__tooltipedText')).not.toExist();
    });
});

function getOfferWithSearchPosition(position?: number) {
    position = position || 100;
    return cloneOfferWithHelpers(offerMock).withSearchPositions([ {
        positions: [
            { position, sort: OfferPosition_OrderedPosition_Sort.RELEVANCE, total_count: 150 },
        ],
        total_count: 200,
    } ]).value();
}

function shallowRenderComponent(props?: any, store?: any) {
    props = props || defaultProps;
    store = store || { bunker: {} };
    return shallow(
        <OfferSnippetSearchRelevancePosition { ...props }/>,
        { context: { ...customContext, store: mockStore(store) } },
    ).dive();
}
