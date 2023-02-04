import {config} from '../../config';
import assert from '../../utils/assert';
import abcProvider from '../../providers/abc';

const MIN_DEVOPS_MEMBER_COUNT = 3;

config.runForServices('abc/devops-scope', ({slug, check}) => {
    /**
     * @description
     * Requires minimal number of DevOps in ABC service.
     *
     * ## Rationale
     * "Bus factor" for each service should be enough for the service to be maintainable even if one (or even two)
     * of the crucial developers are absent (holiday, illness, and etc.).
     *
     * ## Solution
     * You have to follow the following instructions:
     * 1. Go to your ABC service page - `https://abc.yandex-team.ru/services/${service-slug}/duty/`, just replace service-slug with you service's slug.
     * 1. Add people with DevOps role to the ABC service.
     */
    check('ABC_DEVOPS_COUNT', async () => {
        const devops = await abcProvider.getMembers({
            service__slug: slug,
            role__scope: 'devops',
            fields: 'person.login'
        });
        assert(
            devops.results.length >= MIN_DEVOPS_MEMBER_COUNT,
            `Number of DevOps should be greater or equal ${MIN_DEVOPS_MEMBER_COUNT}`
        );
    });
});
