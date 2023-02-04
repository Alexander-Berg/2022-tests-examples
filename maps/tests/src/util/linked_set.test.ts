import {expect} from 'chai';
import LinkedSet from '../../../src/vector_render_engine/util/linked_set';

describe('linked set', () => {
    let set: LinkedSet<number>;

    beforeEach(() => {
        set = new LinkedSet();
    });

    it('should add/remove elements', () => {
        set.insert(10);
        set.insert(13);
        expect([...set]).to.be.deep.equal([10, 13]);

        set.insertBefore(13, 30);
        expect([...set]).to.be.deep.equal([10, 30, 13]);

        set.insertBefore(10, 31);
        expect([...set]).to.be.deep.equal([31, 10, 30, 13]);

        set.insertAfter(10, 40);
        expect([...set]).to.be.deep.equal([31, 10, 40, 30, 13]);

        set.insertAfter(13, 41);
        expect([...set]).to.be.deep.equal([31, 10, 40, 30, 13, 41]);

        set.remove(10);
        expect([...set]).to.be.deep.equal([31, 40, 30, 13, 41]);

        set.remove(31);
        set.remove(13);
        set.remove(40);
        set.remove(30);
        expect([...set]).to.be.deep.equal([41]);

        set.remove(41);
        expect([...set]).to.be.deep.equal([]);
    });

    it('should not add already added items', () => {
        set.insert(10);
        set.insert(10);
        set.insert(13);
        set.insert(10);

        expect([...set]).to.be.deep.equal([10, 13]);
    });

});
