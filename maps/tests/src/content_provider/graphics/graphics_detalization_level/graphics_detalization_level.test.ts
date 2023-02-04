import {expect} from 'chai';
import {GraphicsDetalizationLevel} from 'vector_render_engine/content_provider/graphics/graphics_detaliztion_level/graphics_detalization_level';
import {GraphicsObject} from 'vector_render_engine/content_provider/graphics/graphics_object/graphics_object';
import {GraphicsGroup} from 'vector_render_engine/content_provider/graphics/graphics_group/graphics_group';

describe('GraphicsDetalizationLevel', () => {
    let object: GraphicsObject;
    let group: GraphicsGroup;
    
    beforeEach(() => {
        group = new GraphicsGroup('group', {zIndex: 0, opacity: 1}, null);
        object = new GraphicsObject('object', group);
    });
    
    it('should track whether it has content', () => {
        const level = new GraphicsDetalizationLevel('lod', object, 0, 0, 0);
        expect(level.hasContent).to.equal(false);

        level.addContent({primitives: {}});
        expect(level.hasContent).to.equal(true);
    });
});
