import config from '../../config';
import assert from '../../utils/assert';

config.abcSlugs.forEach((slug) => {
    it(`[${slug}] check resource in deploy`, () => {
        assert(true, 'ABC service should have resources', {slug});
    });
});
