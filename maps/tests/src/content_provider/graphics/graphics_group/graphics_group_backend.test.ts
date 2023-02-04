import {expect} from 'chai';
import {GraphicsGroupBackend} from 'vector_render_engine/content_provider/graphics/graphics_group/graphics_group_backend';

describe('GraphicsGroupBackend', () => {
    it('should add child', () => {
        const group = new GraphicsGroupBackend('group', {zIndex: 0, opacity: 1}, null);
        expect(group.children.size).to.equal(0);

        const child = new GraphicsGroupBackend('child', {zIndex: 0, opacity: 1}, group);
        group.addChild(child);
        expect(group.children.has(child)).to.equal(true);
    });

    it('should remove child', () => {
        const group = new GraphicsGroupBackend('group', {zIndex: 0, opacity: 1}, null);
        const child = new GraphicsGroupBackend('child', {zIndex: 0, opacity: 1}, group);
        group.addChild(child);

        group.removeChild(child);
        expect(group.children.size).to.equal(0);
    });
});
