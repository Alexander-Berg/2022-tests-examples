import { ISiteCardMobile } from 'realty-core/types/siteCard';

export const getSiteCard = () => {
    return {
        resaleTotalOffers: 1,
        resaleOffersPriceInfo: {
            perOffer: {
                from: '100000000',
                to: '100000000',
            },
            perMeter: {
                from: '0',
                to: '0',
            },
        },
    } as ISiteCardMobile;
};
