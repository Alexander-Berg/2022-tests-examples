import { ApartmentType } from 'realty-core/types/siteCard';

export const linkMock = () => '';

export const getItems = ({
    flatStatus = 'PRIMARY_SALE',
    offerType = 'SELL',
}: {
    flatStatus?: string;
    offerType?: string;
}) => ({
    offerId: '123',
    flatStatus,
    offerType,
});

export const getSite = ({ apartmentType }: { apartmentType: ApartmentType }) => ({
    id: 1234,
    buildingFeatures: {
        apartmentType,
    },
    location: {
        settlementRgid: 417899,
    },
    name: 'ЦДС «Приневский»',
    price: {
        from: 6350300,
        currency: 'RUR',
        rooms: {
            '1': { soldout: false, currency: 'RUR', hasOffers: false },
            '2': {
                soldout: false,
                from: 6350300,
                to: 7350300,
                currency: 'RUR',
                areas: { from: '51.9', to: '51.9' },
                hasOffers: true,
                status: 'ON_SALE',
            },
            '3': { soldout: false, currency: 'RUR', hasOffers: false },
            OPEN_PLAN: { soldout: false, currency: 'RUR', hasOffers: false },
            STUDIO: { soldout: false, currency: 'RUR', hasOffers: false },
            PLUS_4: { soldout: false, currency: 'RUR', hasOffers: false },
        },
        totalOffers: 3,
    },
});
