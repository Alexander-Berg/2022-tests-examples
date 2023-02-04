import {config, ABC_TAG_ID} from '../../config';
import assert from '../../utils/assert';
import abcProvider from '../../providers/abc';
import {ServiceTag} from '@yandex-int/maps._abc';

config.runForServices('abc/service-tags', ({slug, check}) => {
    let abcTags: ServiceTag[];

    before(async () => {
        abcTags = (await abcProvider.getServices({slug: slug, fields: 'tags'})).results[0].tags;
    });

    /**
     * @description
     * All our services should be marked as external or internal.  Tags in ABC are used for this. If your balancer is open to the outside, then you need to put an external tag, if the balancer is internal, then put the appropriate tag
     *
     * ## Reason
     * Some of our guidelines depend on whether it is an external service or an internal one. Therefore, you need to place them correctly
     *
     * ## Solution
     * 1) Go to ABC
     * 2) Tap "Edit tags" The link under the name of the ABC service
     * 3) Choose "external" or "internal" from list
     */
    check('ABC_REQUIRED_TAGS', async () => {
        const requiredTags = abcTags.filter((currentTag) =>
            currentTag.id === ABC_TAG_ID.external || currentTag.id === ABC_TAG_ID.internal
        );

        assert(requiredTags.length !== 0, 'The service must have one of the following tags: external/internal');
    });
});
