import { ISiteCard } from 'realty-core/types/siteCard';

export const card = ({
    offerStat: {
        nonPrimarySaleStats: {
            byRoomsType: [
                {
                    roomsType: {
                        studio: {},
                    },
                    stats: {
                        apartmentsCount: 3,
                        areaRange: {
                            unit: 'SQ_M',
                            from: 24.1,
                            to: 34.6,
                        },
                        priceInfo: {
                            perOffer: {
                                currency: 'RUB',
                                from: '8300000',
                                to: '12000000',
                            },
                            perMeter: {
                                currency: 'RUB',
                                from: '319391',
                                to: '346820',
                            },
                        },
                        floor: [9, 10, 12],
                        commissioningDate: [
                            {
                                year: 2024,
                                quarter: 2,
                                constructionState: 'CONSTRUCTION_STATE_UNFINISHED',
                            },
                            {
                                year: 2024,
                                quarter: 3,
                                constructionState: 'CONSTRUCTION_STATE_UNFINISHED',
                            },
                        ],
                        offersCount: 3,
                        hasVirtualTour: false,
                    },
                },
                {
                    roomsType: {
                        rooms: {
                            count: 1,
                        },
                    },
                    stats: {
                        apartmentsCount: 3,
                        areaRange: {
                            unit: 'SQ_M',
                            from: 37.2,
                            to: 44.4,
                        },
                        priceInfo: {
                            perOffer: {
                                currency: 'RUB',
                                from: '11000000',
                                to: '12800000',
                            },
                            perMeter: {
                                currency: 'RUB',
                                from: '288288',
                                to: '308900',
                            },
                        },
                        floor: [4, 6, 13],
                        commissioningDate: [
                            {
                                year: 2024,
                                quarter: 2,
                                constructionState: 'CONSTRUCTION_STATE_UNFINISHED',
                            },
                        ],
                        offersCount: 3,
                        hasVirtualTour: false,
                    },
                },
                {
                    roomsType: {
                        rooms: {
                            count: 2,
                        },
                    },
                    stats: {
                        apartmentsCount: 1,
                        areaRange: {
                            unit: 'SQ_M',
                            from: 56.9,
                            to: 56.9,
                        },
                        priceInfo: {
                            perOffer: {
                                currency: 'RUB',
                                from: '15000000',
                                to: '15000000',
                            },
                            perMeter: {
                                currency: 'RUB',
                                from: '263620',
                                to: '263620',
                            },
                        },
                        floor: [6],
                        commissioningDate: [
                            {
                                year: 2024,
                                quarter: 2,
                                constructionState: 'CONSTRUCTION_STATE_UNFINISHED',
                            },
                        ],
                        offersCount: 1,
                        hasVirtualTour: false,
                    },
                },
                {
                    roomsType: {
                        rooms: {
                            count: 3,
                        },
                    },
                    stats: {
                        apartmentsCount: 1,
                        areaRange: {
                            unit: 'SQ_M',
                            from: 78.6,
                            to: 78.6,
                        },
                        priceInfo: {
                            perOffer: {
                                currency: 'RUB',
                                from: '21300000',
                                to: '21300000',
                            },
                            perMeter: {
                                currency: 'RUB',
                                from: '270992',
                                to: '270992',
                            },
                        },
                        floor: [12],
                        commissioningDate: [
                            {
                                year: 2024,
                                quarter: 2,
                                constructionState: 'CONSTRUCTION_STATE_UNFINISHED',
                            },
                        ],
                        offersCount: 1,
                        hasVirtualTour: false,
                    },
                },
            ],
        },
    },
} as unknown) as ISiteCard;
