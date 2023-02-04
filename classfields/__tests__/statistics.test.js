import { agencyOffer } from 'view/__fixtures__/offer';
import { shouldShowCallsStatistics } from '../statistics';

describe('Offer statistics helper', () => {
    describe('shouldShowCallsStatistics ', () => {
        it('returns true if offer has calls', () => {
            const redirectPhones = false;
            const offer = {
                ...agencyOffer,
                isFromFeed: false,
                stats: {
                    ...agencyOffer.stats,
                    calls: {
                        status: 'success',
                        total: 1,
                        details: []
                    }
                }
            };

            expect(shouldShowCallsStatistics(offer, redirectPhones)).toBeTruthy();
        });

        it('returns true if offer calls statistic is available', () => {
            const redirectPhones = false;
            const offer = {
                ...agencyOffer,
                isFromFeed: false,
                stats: {
                    ...agencyOffer.stats,
                    calls: {
                        status: 'success',
                        total: 1,
                        details: []
                    }
                }
            };

            expect(shouldShowCallsStatistics(offer, redirectPhones)).toBeTruthy();
        });

        it('returns true if offer calls statistic is already enabled', () => {
            const redirectPhones = true;
            const offer = {
                ...agencyOffer,
                isFromFeed: false,
                stats: {
                    ...agencyOffer.stats,
                    calls: {
                        status: 'success',
                        total: 0,
                        details: []
                    }
                }
            };

            expect(shouldShowCallsStatistics(offer, redirectPhones)).toBeTruthy();
        });

        it('returns true if offer is from feed', () => {
            const redirectPhones = false;
            const offer = {
                ...agencyOffer,
                isFromFeed: true,
                stats: {
                    ...agencyOffer.stats,
                    calls: {
                        status: 'success',
                        total: 0,
                        details: []
                    }
                }
            };

            expect(shouldShowCallsStatistics(offer, redirectPhones)).toBeTruthy();
        });

        it('returns false if offer calls statistic is not available and not enabled and empty and offer not from feed',
            () => {
                const redirectPhones = false;
                const offer = {
                    ...agencyOffer,
                    isFromFeed: false,
                    stats: {
                        ...agencyOffer.stats,
                        calls: {
                            status: 'success',
                            total: 0,
                            details: []
                        }
                    }
                };

                expect(shouldShowCallsStatistics(offer, redirectPhones)).toBeFalsy();
            }
        );
    });
});
