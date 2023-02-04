import {expect} from 'chai';
import {ArrayStorage} from '../../../src/vector_render_engine/util/array_storage';

describe('ArrayStorage', () => {
    it('iterate over added items', () => {
        const item1 = {};
        const item2 = {};
        const arrayStorage = new ArrayStorage<{}>();
        arrayStorage.add(item1);
        arrayStorage.add(item2);

        const items = [...arrayStorage];
        expect(items.length).to.be.equal(2);
        expect(items[0]).to.be.equal(item1);
        expect(items[1]).to.be.equal(item2);
    });

    it('delete items', () => {
        const item1 = {};
        const item2 = {};
        const arrayStorage = new ArrayStorage<{}>();
        arrayStorage.add(item1);
        arrayStorage.add(item2);

        arrayStorage.remove(item1);

        const items1 = [...arrayStorage];
        expect(items1.length).to.be.equal(1);
        expect(items1[0]).to.be.equal(item2);

        arrayStorage.remove(item2);

        const items2 = [...arrayStorage];
        expect(items2.length).to.be.equal(0);
    });
});
