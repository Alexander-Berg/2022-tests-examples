import {expect} from 'chai';
import {SortedArrayStorage} from '../../../src/vector_render_engine/util/sorted_array_storage';
import Comparator from '../../../src/vector_render_engine/util/comparator';

interface Item {
    key: number;
}

describe('SortedArrayStorage', () => {
    let storage: SortedArrayStorage<Item>;

    beforeEach(() => {
        storage = new SortedArrayStorage((a, b) => a.key - b.key);
    });

    it('should sort items', () => {
        const item1 = {key: 3};
        const item2 = {key: 1};
        const item3 = {key: 2};

        storage.add(item1);
        storage.add(item2);
        storage.add(item3);

        const items = [...storage];
        expect(items.length).to.equal(3);
        expect(items[0]).to.equal(item2);
        expect(items[1]).to.equal(item3);
        expect(items[2]).to.equal(item1);
    });

    it('should remove items', () => {
        const item1 = {key: 10};
        const item2 = {key: 1};
        storage.add(item1);
        storage.add(item2);

        storage.remove(item1);

        const items = [...storage];
        expect(items.length).to.equal(1);
        expect(items[0]).to.equal(item2);
    });
});
