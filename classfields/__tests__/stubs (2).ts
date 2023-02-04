import { generateImageUrl } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

import { IOfferCard } from 'realty-core/types/offerCard';

export const ownerOffer = ({
    isEditable: true,
    offerType: 'SELL',
    offerCategory: 'APARTMENT',
    offerStatus: 'active',
    offerId: '3584237142849818368',
    price: {
        currency: 'RUR',
        period: 'PER_MONTH',
        value: 6777888,
        valuePerPart: 101163,
        unitPerPart: 'SQUARE_METER',
    },
    author: {
        id: '7777777777',
        category: 'OWNER',
        photo: generateImageUrl({ width: 48, height: 48 }),
        agentName: 'Александр Сергеевич',
        creationDate: '2019-04-01T03:00:00.111Z',
    },
    uid: '7777777777',
    location: {
        rgid: 176337,
        geoId: 322,
    },
} as unknown) as IOfferCard;
