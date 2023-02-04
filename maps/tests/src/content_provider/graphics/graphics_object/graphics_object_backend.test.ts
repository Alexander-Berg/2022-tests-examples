import {expect} from 'chai';
import {GraphicsGroupBackend} from 'vector_render_engine/content_provider/graphics/graphics_group/graphics_group_backend';
import {GraphicsObjectBackend} from '../../../../../src/vector_render_engine/content_provider/graphics/graphics_object/graphics_object_backend';

describe('GraphcisObjectBackend', () => {
    let object: GraphicsObjectBackend;
    let root: GraphicsGroupBackend;

    beforeEach(() => {
        root = new GraphicsGroupBackend('root', {zIndex: 0, opacity: 1}, null);
        object = new GraphicsObjectBackend('object', {} as any, {zIndex: 0, simplificationRate: 0}, root);
    });

    it('should update geometry and style, increment version', () => {
        const feature = {} as any;
        const style = {zIndex: 10, simplificationRate: 5};
        const versionBefore = object.version;

        object.update(feature, style);

        const versionAfter = object.version;
        expect(object.feature).to.equal(feature);
        expect(object.style).to.equal(style);
        expect(versionAfter).to.be.greaterThan(versionBefore);
    });
});
