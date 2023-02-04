import * as assert from 'assert';
import {SearchTreeWithFallback} from '../../server/editor/format-utils/search-tree-with-fallback';

describe('SearchTreeWithFallback class', () => {
    it('should throw if initialized with negative integer', () => {
        assert.throws(() => {
            // tslint:disable-next-line
            new SearchTreeWithFallback(-10);
        });
    });

    it('should fetch a value from a single level depth', () => {
        const tree = new SearchTreeWithFallback(1);
        const keys = ['1'];
        const value = 'value';
        tree.add(keys, value);

        assert.equal(tree.get(keys), value);
    });

    it('should fetch null if no default keys were created', () => {
        const tree = new SearchTreeWithFallback(1);
        const keys = ['1'];
        const value = 'value';
        tree.add(keys, value);

        const otherKeys = ['2'];
        assert.equal(tree.get(otherKeys), null);
    });

    it('should fetch the default value if no such key is present', () => {
        const tree = new SearchTreeWithFallback(1);
        const defaultKeys: string[] = [];
        const value = 'value';
        tree.add(defaultKeys, value);

        const otherKeys = ['2'];
        assert.equal(tree.get(otherKeys), value);
    });

    it('should fetch a value from a multilevel level depth', () => {
        const tree = new SearchTreeWithFallback(2);
        const keys = ['1', '2'];
        const value = 'value';
        tree.add(keys, value);

        assert.equal(tree.get(keys), value);
    });

    it('should fetch the default value if no such key is present in multilevel', () => {
        const tree = new SearchTreeWithFallback(2);
        const defaultKeys = ['2', ''];
        const value = 'value';
        tree.add(defaultKeys, value);

        const otherKeys = ['2', '1'];
        assert.equal(tree.get(otherKeys), value);
    });

    it('should fetch the default value if no key is present in the middle of the path of a multilevel tree', () => {
        const tree = new SearchTreeWithFallback(3);
        const defaultKeys = ['2', '', '5'];
        const value = 'value';
        tree.add(defaultKeys, value);

        const otherKeys = ['2', '1', '5'];
        assert.equal(tree.get(otherKeys), value);
    });

    it('should be able to tell if there is certain subtree with .has()', () => {
        const tree = new SearchTreeWithFallback(3);
        const defaultKeys = ['2', '1', '5'];
        tree.add(defaultKeys, 'value');

        assert.equal(tree.has(defaultKeys), true);
        assert.equal(tree.has(defaultKeys.slice(0, -1)), true);

        const otherKeys = ['2', '1', '6'];
        assert.equal(tree.has(otherKeys), false);
    });

    it('should throw on attempts to temper with a wildcard on any level', () => {
        const tree = new SearchTreeWithFallback(3);
        const keysWithWildcard = ['2', SearchTreeWithFallback.wildcard, '5'];

        assert.throws(() => {
            tree.add(keysWithWildcard, 'value');
        });
    });

    it('should allow for falsy-values on the bottom level', () => {
        const tree = new SearchTreeWithFallback(2);
        const keysFalse = ['1', '2'];
        tree.add(keysFalse, false);
        assert.equal(tree.has(keysFalse), true);
        assert.strictEqual(tree.get(keysFalse), false);

        const keysZero = ['1', 'zero'];
        tree.add(keysZero, 0);
        assert.equal(tree.has(keysZero), true);
        assert.strictEqual(tree.get(keysZero), 0);

        const keysNull = ['1', 'null'];
        tree.add(keysNull, null);
        assert.equal(tree.has(keysNull), true);
        assert.strictEqual(tree.get(keysNull), null);

        const keysEmpty = ['1', 'empty'];
        tree.add(keysEmpty, '');
        assert.equal(tree.has(keysEmpty), true);
        assert.strictEqual(tree.get(keysEmpty), '');
    });

    it('should be able to use the wildcard in has() calls', () => {
        const tree = new SearchTreeWithFallback(3);
        const keysWithAHole: (string | undefined)[] = ['2', undefined, '5'];
        const value = 'value';
        tree.add(keysWithAHole, 'value');

        const keysWithWildcard = ['2', SearchTreeWithFallback.wildcard, '5'];
        assert.equal(tree.get(keysWithWildcard), value);

        const keysWithNoPrefix = [undefined, '1', '2'];
        const value2 = 'value2';
        tree.add(keysWithNoPrefix, value2);
        assert.equal(tree.has([SearchTreeWithFallback.wildcard, '1', '2']), true);
    });
});
