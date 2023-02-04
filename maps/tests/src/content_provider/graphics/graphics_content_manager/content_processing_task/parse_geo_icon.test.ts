import {expect} from 'chai';
import {parseGeoIcon} from '../../../../../../src/vector_render_engine/content_provider/graphics/content_manager/content_processing_task/parsers/parse_geo_icon';

const METADATA = new Map([['key', 'value']]);

describe('parsers/parse_geo_icon', () => {
    describe('parseGeoIcon', () => {
        it('should parse icon', () => {
            const icons = parseGeoIcon({
                type: 'Point',
                coordinates: {x: 0, y: 0}
            }, {
                'icon-image': 'http://url.com'
            }, 0, METADATA);

            expect(icons.length).to.equal(1);
            expect(icons[0].id).to.equal(0);
            expect(icons[0].metadata).to.deep.equal(METADATA);
            expect(icons[0].position).to.deep.equal({x: 0, y: 0});
            expect(icons[0].styles[0].url).to.equal('http://url.com');
        });
    });
});
