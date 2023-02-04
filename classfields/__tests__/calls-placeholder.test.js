import { agencyOffer } from 'view/__fixtures__/offer';
import { getPlaceholderType } from '../calls-placeholder';

describe('CallsPlaceholder', () => {
    describe('getPlaceholderType ', () => {
        it('returns "no_stats" if redirect enabled and no calls and no views', () => {
            const offer = {
                ...agencyOffer,
                redirectPhones: true,
                stats: {
                    ...agencyOffer.stats,
                    offerShow: {
                        status: 'success',
                        total: 0,
                        details: []
                    },
                    calls: {
                        status: 'success',
                        total: 0,
                        details: []
                    }
                }
            };

            const result = getPlaceholderType(offer);

            expect(result).toEqual('no_stats');
        });

        it('returns "none" if redirect enabled and offer has calls', () => {
            const offer = {
                ...agencyOffer,
                redirectPhones: true,
                stats: {
                    ...agencyOffer.stats,
                    offerShow: {
                        status: 'success',
                        total: 0,
                        details: []
                    },
                    calls: {
                        status: 'success',
                        total: 1,
                        details: []
                    }
                }
            };

            const result = getPlaceholderType(offer);

            expect(result).toEqual('none');
        });

        it('returns "none" if redirect enabled and offer has views', () => {
            const offer = {
                ...agencyOffer,
                redirectPhones: true,
                stats: {
                    ...agencyOffer.stats,
                    offerShow: {
                        status: 'success',
                        total: 1,
                        details: []
                    },
                    calls: {
                        status: 'success',
                        total: 0,
                        details: []
                    }
                }
            };

            const result = getPlaceholderType(offer);

            expect(result).toEqual('none');
        });

        it('returns "none" if offer is from feed and has calls', () => {
            const offer = {
                ...agencyOffer,
                redirectPhones: false,
                isFromFeed: true,
                stats: {
                    ...agencyOffer.stats,
                    calls: {
                        status: 'success',
                        total: 1,
                        details: []
                    }
                }
            };

            const result = getPlaceholderType(offer);

            expect(result).toEqual('none');
        });

        it('returns "none" if offer is from feed and has views', () => {
            const offer = {
                ...agencyOffer,
                redirectPhones: false,
                isFromFeed: true,
                stats: {
                    ...agencyOffer.stats,
                    offerShow: {
                        status: 'success',
                        total: 1,
                        details: []
                    },
                    calls: {
                        status: 'success',
                        total: 0,
                        details: []
                    }
                }
            };

            const result = getPlaceholderType(offer);

            expect(result).toEqual('none');
        });

        it('returns "no_stats" if offer is from feed and has no calls and views', () => {
            const offer = {
                ...agencyOffer,
                redirectPhones: false,
                isFromFeed: true,
                stats: {
                    ...agencyOffer.stats,
                    offerShow: {
                        status: 'success',
                        total: 0,
                        details: []
                    },
                    calls: {
                        status: 'success',
                        total: 0,
                        details: []
                    }
                }
            };

            const result = getPlaceholderType(offer);

            expect(result).toEqual('no_stats');
        });
    });
});
