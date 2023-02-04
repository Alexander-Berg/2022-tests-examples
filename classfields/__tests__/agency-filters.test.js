import merge from 'lodash/merge';
import { getFiltersVisibility } from '../agency-filters';

const state = {
    filters: { fields: { status: 'ANY' } },
    offersNew: {
        counts: {
            published: {
                total: 68,
                rent: 12,
                sell: 56,
                services: { ANY: 68, YES: 23, NO: 45 },
                fromFeed: { ANY: 68, YES: 14, NO: 54 },
                byCategory: {
                    ANY: 68,
                    APARTMENT: 22,
                    ROOMS: 3,
                    HOUSE: 28,
                    LOT: 2,
                    GARAGE: 4,
                    COMMERCIAL: 9
                },
                copies: {
                    ANY: 0,
                    YES: 13
                }
            },
            moderation: {
                total: 0,
                rent: 0,
                sell: 0,
                services: { ANY: 0, YES: 0, NO: 0 },
                fromFeed: { ANY: 0, YES: 0, NO: 0 },
                byCategory: {
                    ANY: 0,
                    APARTMENT: 0,
                    ROOMS: 0,
                    HOUSE: 0,
                    LOT: 0,
                    GARAGE: 0,
                    COMMERCIAL: 0
                },
                copies: {
                    ANY: 0,
                    YES: 0
                }
            },
            unpublished: {
                total: 44,
                rent: 7,
                sell: 37,
                services: { ANY: 44, YES: 0, NO: 44 },
                fromFeed: { ANY: 44, YES: 4, NO: 40 },
                byCategory: {
                    ANY: 44,
                    APARTMENT: 22,
                    ROOMS: 3,
                    HOUSE: 2,
                    LOT: 1,
                    GARAGE: 4,
                    COMMERCIAL: 12
                },
                copies: {
                    ANY: 0,
                    YES: 0
                }
            },
            banned: {
                total: 1,
                rent: 0,
                sell: 1,
                services: { ANY: 1, YES: 0, NO: 1 },
                fromFeed: { ANY: 1, YES: 0, NO: 1 },
                byCategory: {
                    ANY: 1,
                    APARTMENT: 1,
                    ROOMS: 0,
                    HOUSE: 0,
                    LOT: 0,
                    GARAGE: 0,
                    COMMERCIAL: 0
                },
                copies: {
                    ANY: 0,
                    YES: 0
                }
            },
            all: {
                total: 113,
                rent: 19,
                sell: 94,
                services: { ANY: 113, YES: 23, NO: 90 },
                fromFeed: { ANY: 113, YES: 18, NO: 95 },
                byCategory: {
                    ANY: 113,
                    APARTMENT: 45,
                    ROOMS: 6,
                    HOUSE: 30,
                    LOT: 3,
                    GARAGE: 8,
                    COMMERCIAL: 21
                },
                copies: {
                    ANY: 113,
                    YES: 13
                }
            }
        }
    }
};

describe('getFiltersVisibility', () => {
    it('all filters must be visible', () => {
        expect(getFiltersVisibility(
            merge({}, state, {
                offersNew: {
                    feedsList: [ {} ]
                }
            })
        )).toEqual({
            offerType: true,
            copies: true,
            category: true,
            partner: true,
            xml: true,
            services: true
        });
    });

    it('partner filter not visible if user has no feeds', () => {
        expect(getFiltersVisibility(
            merge({}, state, {
                offersNew: {
                    feedsList: []
                }
            })
        )).toEqual({
            offerType: true,
            copies: true,
            category: true,
            partner: false,
            xml: true,
            services: true
        });
    });

    it('xml filter not visible if user has no feed offers', () => {
        expect(getFiltersVisibility(
            merge({}, state, {
                offersNew: {
                    feedsList: [],
                    counts: {
                        all: {
                            fromFeed: { ANY: 113, YES: 0, NO: 113 }
                        }
                    }
                }
            })
        )).toEqual({
            offerType: true,
            copies: true,
            category: true,
            partner: false,
            xml: false,
            services: true
        });
    });

    it('offerType filter not visible if has no sell or rent offers', () => {
        expect(getFiltersVisibility(
            merge({}, state, {
                filters: { fields: { status: 'moderation' } }
            })
        )).toEqual({
            offerType: false,
            copies: false,
            category: false,
            partner: false,
            xml: true,
            services: false
        });
    });

    it('copies filter not visible if has no copies', () => {
        expect(getFiltersVisibility(
            merge({}, state, {
                filters: { fields: { status: 'moderation' } }
            })
        )).toEqual({
            offerType: false,
            copies: false,
            category: false,
            partner: false,
            xml: true,
            services: false
        });
    });
});
