import {expect} from 'chai';
import {BinarySearchTree} from '../../../src/vector_render_engine/util/binary_tree';

describe('binary tree', () => {
    describe('BST', () => {
        let tree: BinarySearchTree<number>;

        beforeEach(() => {
            tree = new BinarySearchTree<number>((a: number, b: number) => a - b);
        });

        it('should add/remove elements', () => {
            expect(tree.size).to.be.equal(0);
            tree.insert(3);
            const n1 = tree.insert(1);
            tree.insert(2);
            tree.remove(n1);
            expect(tree.size).to.be.equal(3 - 1);
        });

        it('should return min/max elements', () => {
            tree.insert(3);
            tree.insert(5);
            tree.insert(1);
            tree.insert(1);

            expect(tree.min).to.be.equal(1);
            expect(tree.max).to.be.equal(5);
        });

        it('should remove arbitrary element', () => {
            const n3 = tree.insert(3);
            const n5 = tree.insert(5);
            const n1 = tree.insert(1);
            const n10 = tree.insert(10);
            const n7 = tree.insert(7);
            const n6 = tree.insert(6);

            tree.remove(n5);
            expect([...tree]).to.be.deep.equal([1, 3, 6, 7, 10]);

            tree.remove(n1);
            expect([...tree]).to.be.deep.equal([3, 6, 7, 10]);

            tree.remove(n6);
            expect([...tree]).to.be.deep.equal([3, 7, 10]);

            tree.remove(n10);
            expect([...tree]).to.be.deep.equal([3, 7]);

            tree.remove(n3);
            expect([...tree]).to.be.deep.equal([7]);

            tree.remove(n7);
            expect([...tree]).to.be.deep.equal([]);
        });

    });

});
