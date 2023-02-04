export const getSitePlan = ({
    isStudio = false,
    offersCount = 2,
    floors = [ 2, 7, 9 ],
    withImages = true,
    withKitchenArea = true,
    commissioningDate
} = {}) => ({
    roomType: isStudio ? 'STUDIO' : 'PLUS_4',
    roomCount: 5,
    wholeArea: {
        value: 77.2,
        unit: 'SQ_M'
    },
    kitchenArea: withKitchenArea && {
        value: 17.5,
        unit: 'SQ_M'
    },
    images: withImages ? {
        large: '//avatars.mdst.yandex.net/get-realty/3043/offer.8905999522369345750.8171355523621390876/large'
    } : {},
    clusterId: '166185-A85D3D96DA13D444',
    floors,
    commissioningDate: commissioningDate || [
        {
            year: 2020,
            quarter: 3,
            constructionState: 'CONSTRUCTION_STATE_UNKNOWN'
        },
        {
            year: 2021,
            quarter: 4,
            constructionState: 'CONSTRUCTION_STATE_UNKNOWN'
        }
    ],
    pricePerOffer: {
        currency: 'RUB',
        ...(
            offersCount === 1 ?
                {
                    from: '9642280',
                    to: '9642280'
                } :
                {
                    from: '9642280',
                    to: '10012840'
                }
        )
    },
    pricePerMeter: {
        currency: 'RUB',
        from: '124900',
        to: '129700'
    },
    offersCount,
    offerId: '6302897158557507297'
});
