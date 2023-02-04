import { findPrevSelectedOffer } from '../';

describe('findPrevSelectedOffer', () => {
    it('returns the last selected offer which is placed before current one', () => {
        const offers = [ {}, { selected: true }, {}, {} ];

        expect(findPrevSelectedOffer(3, offers)).toEqual({ selected: true });
    });

    it('returns the last selected offer from a cluster which is placed before current selected offer', () => {
        const offers = [ {}, { clusterOffers: [ { selected: true } ] }, {}, {} ];

        expect(findPrevSelectedOffer(3, offers)).toEqual({ clusterOffers: [ { selected: true } ] });
    });

    it('does not return an offer if current offer is placed before selected one', () => {
        const offers = [ {}, {}, {}, { selected: true } ];

        expect(findPrevSelectedOffer(3, offers)).toBeUndefined();
    });

    it('does not return an offer if there are no selected offers', () => {
        const offers = [ {}, {}, {}, {} ];

        expect(findPrevSelectedOffer(3, offers)).toBeUndefined();
    });
});
