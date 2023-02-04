import { ContextBlock, ContextPage } from '@vertis/schema-registry/ts-types-snake/auto/api/stat_events';

import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import cardMock from 'auto-core/react/dataDomain/card/mocks/card.cars.mock';

import getSelfType from './getSelfType';

it('вернет TYPE_GROUP если размер группы у оффера больше одного', () => {
    const result = getSelfType(
        cloneOfferWithHelpers(cardMock).withGroupingInfo({ size: 2 }).value(),
    );
    expect(result).toBe('TYPE_GROUP');
});

it('вернет TYPE_GROUP для блока с прочими конфигурациями', () => {
    const result = getSelfType(cardMock, {
        block: ContextBlock.BLOCK_OTHER_CONFIGURATION,
    });
    expect(result).toBe('TYPE_GROUP');
});

describe('TYPE_PREMIUM', () => {
    it('вернет TYPE_PREMIUM если у объявы дилера есть корона и блок листинг', () => {
        const result = getSelfType(
            cloneOfferWithHelpers(cardMock).withSellerTypeCommercial().withTags([ 'type_top_crown' ]).value(),
            { page: ContextPage.PAGE_LISTING },
        );
        expect(result).toBe('TYPE_PREMIUM');
    });

    it('вернет TYPE_SINGLE если у объявы дилера нет короны и блок листинг', () => {
        const result = getSelfType(
            cloneOfferWithHelpers(cardMock).withSellerTypeCommercial().value(),
            { page: ContextPage.PAGE_LISTING },
        );
        expect(result).toBe('TYPE_SINGLE');
    });

    it('вернет TYPE_SINGLE если у объявы дилера есть корона но блок не-листинг', () => {
        const result = getSelfType(
            cloneOfferWithHelpers(cardMock).withSellerTypeCommercial().withTags([ 'type_top_crown' ]).value(),
            { page: ContextPage.PAGE_CARD },
        );
        expect(result).toBe('TYPE_SINGLE');
    });
});

describe('TYPE_TOP', () => {
    it('вернет TYPE_TOP если у объявы частника есть корона и блок листинг', () => {
        const result = getSelfType(
            cloneOfferWithHelpers(cardMock).withSellerTypePrivate().withTags([ 'type_top_crown' ]).value(),
            { page: ContextPage.PAGE_LISTING },
        );
        expect(result).toBe('TYPE_TOP');
    });

    it('вернет TYPE_SINGLE если у объявы частника нет короны и блок листинг', () => {
        const result = getSelfType(
            cloneOfferWithHelpers(cardMock).withSellerTypePrivate().value(),
            { page: ContextPage.PAGE_LISTING },
        );
        expect(result).toBe('TYPE_SINGLE');
    });

    it('вернет TYPE_SINGLE если у объявы частника есть корона но блок не-листинг', () => {
        const result = getSelfType(
            cloneOfferWithHelpers(cardMock).withSellerTypePrivate().withTags([ 'type_top_crown' ]).value(),
            { page: ContextPage.PAGE_CARD },
        );
        expect(result).toBe('TYPE_SINGLE');
    });
});

it('в дефолтном кейсе вернет TYPE_SINGLE', () => {
    const result = getSelfType(cardMock);
    expect(result).toBe('TYPE_SINGLE');
});
