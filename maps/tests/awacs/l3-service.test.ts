import {config} from '../../config';
import assert from '../../utils/assert';
import {getNamespaceId} from '../../utils/awacs';
import l3ttProvider from '../../providers/l3tt';

const fetchServiceShortInfo = async (slug: string) => {
    const awacsNamespace = getNamespaceId(slug);
    return l3ttProvider.getServiceShortByFqdn(awacsNamespace);
};

config.runForServices('awacs/l3-service', ({slug, check}) => {
    /**
     * @description
     * ABC slug should start with maps-front- prefix.
     *
     * ## Reason
     * All automatization is connected with ABC slug.
     *
     * ## Solution
     * Discuss solutions with actual responsible for duty: ["DevOps (Инфраструктура фронтенда геосервисов"](https://abc.yandex-team.ru/services/maps-front-infra/?scope=devops)
     */
    check('AWACS_L3_SERVICE_MATCHING_NAME', async () => {
        const serviceShortInfo = await fetchServiceShortInfo(slug);

        assert(
            serviceShortInfo !== null,
            'The l3tt service should have matching name with the awacs service'
        );
    });

    /**
     * @description
     * Awacs L3 service should be owned by maps-front-infra.
     *
     * ## Reason
     * Infrastructure group must have access L3 service for solving problems if it is needed.
     *
     * ## Solution
     * You have to add ["DevOps (Инфраструктура фронтенда геосервисов)"](https://abc.yandex-team.ru/services/maps-front-infra/?scope=devops) group to L3 service access list.
     * If you have not access for this, please contact with actual responsible for duty: ["DevOps (Инфраструктура фронтенда геосервисов"](https://abc.yandex-team.ru/services/maps-front-infra/?scope=devops)
     *
     * {% note info %}
     *
     * See more documentation on https://wiki.yandex-team.ru/awacs/certs/
     *
     * {% endnote %}
     */
    check('AWACS_L3_SERVICE_OWNED_BY_INFRA', async () => {
        const serviceShortInfo = await fetchServiceShortInfo(slug);

        assert(
            serviceShortInfo !== null && serviceShortInfo.abc === 'maps-front-infra',
            'The l3tt service should be owned by maps-front-infra'
        );
    });
});
