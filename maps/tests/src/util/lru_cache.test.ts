import {expect} from 'chai';
import * as chai from 'chai';
import * as sinonChai from 'sinon-chai';
import {LruCache} from '../../../src/vector_render_engine/util/lru_cache';

chai.use(sinonChai);

describe('LruCache', () => {
    let cache: LruCache<number, string>;

    beforeEach(() => {
        cache = new LruCache(5);
    });

    it('should store value by a key', () => {
        const key = 10;
        const value = 'Hello';

        expect(cache.get(key)).to.be.undefined;
        expect(cache.has(key)).to.be.false;

        cache.set(key, value);

        expect(cache.get(key)).to.equal(value);
        expect(cache.has(key)).to.be.true ;
    });

    it('should delete values', () => {
        cache.set(10, 'value of 10');
        cache.set(11, 'value of 11');
        cache.set(12, 'value of 12');

        expect(cache.has(11)).to.be.true;
        expect(cache.delete(11)).to.be.true;
        expect(cache.has(11)).to.be.false;
        expect(cache.delete(11)).to.be.false;

        cache.set(13, 'value of 13');
        cache.set(14, 'value of 14');
        cache.set(15, 'value of 15');
        cache.set(16, 'value of 16');

        expect(cache.has(10)).to.be.false;
        expect(cache.has(12)).to.be.true;
    });

    it('should delete value least recently used value when the size is exceeded', () => {
        cache.set(10, 'value of 10');
        cache.set(11, 'value of 11');
        cache.set(12, 'value of 12');
        cache.set(13, 'value of 13');
        cache.set(10, 'value of 10');
        cache.set(14, 'value of 14');
        cache.set(15, 'value of 15');

        expect(cache.has(10)).to.be.true;
        expect(cache.has(11)).to.be.false;
        expect(cache.has(12)).to.be.true;
        expect(cache.has(13)).to.be.true;
        expect(cache.has(14)).to.be.true;
        expect(cache.has(15)).to.be.true;

        cache.set(16, 'value of 16');

        expect(cache.has(12)).to.be.false;
    });
});
