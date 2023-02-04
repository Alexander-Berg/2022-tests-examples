import {expect} from 'chai';
import {formatDistance, formatDuration} from 'app/lib/format/units';

describe('formatDistance', () => {
    describe('"ru" lang', () => {
        it('should format distance', () => {
            expect(formatDistance(100)).to.equal('100 м');
            expect(formatDistance(8206.33)).to.equal('8,2 км');
            expect(formatDistance(809477.9606080651)).to.equal('810 км');
        });

        it('should format zero distance', () => {
            expect(formatDistance(0)).to.equal('0 м');
        });
    });
});

describe('formatDuration()', () => {
    describe('"ru" lang', () => {
        it('should format hours', () => {
            expect(formatDuration(47116.67663503811)).to.equal('13 ч 5 мин');
            expect(formatDuration(46629.37362289801)).to.equal('12 ч 57 мин');
        });

        it('should format days', () => {
            // Route example: "Москва" - "Сургут"
            expect(formatDuration(169278.04191671312)).to.equal('1 д 23 ч 1 мин');
        });

        it('should round seconds to minutes', () => {
            expect(formatDuration(10)).to.equal('1 мин');
            expect(formatDuration(60)).to.equal('1 мин');
            expect(formatDuration(62)).to.equal('1 мин');
            expect(formatDuration(89)).to.equal('1 мин');
            expect(formatDuration(90)).to.equal('2 мин');
        });

        it('should format zero duration', () => {
            expect(formatDuration(0)).to.equal('0 сек');
        });
    });
});
