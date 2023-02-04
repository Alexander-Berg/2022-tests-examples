import {isEqual} from 'lodash';
import {config} from '../../config';
import awacsProvider, {getNamespacesIds} from '../../providers/awacs';
import staffProvider from '../../providers/staff';
import assert from '../../utils/assert';
import {
    fixAwacsBalancerNoDirectAccess,
    fixAwacsNamespaceOnlyDevopsAccess,
    fixAwacsUpstreamNoDirectAccess
} from './access.fix';

async function getDutyGroupsIds(abcSlug: string): Promise<string[]> {
    const {result: serviceGroups} = await staffProvider.getGroups({
        type: 'servicerole',
        'parent.url': [
            'svc_maps-front-infra',
            `svc_${abcSlug}`
        ].join(','),
        role_scope: 'devops',
        _fields: 'id'
    });

    return serviceGroups
        .map(({id}) => id && id.toString())
        .filter((id): id is string => Boolean(id))
        .sort();
}

config.runForServices('awacs/access', ({slug, check}) => {
    let devopsGroupsIds: string[];
    let namespaceIds: string[];

    before(async () => {
        devopsGroupsIds = await getDutyGroupsIds(slug);
        namespaceIds = await getNamespacesIds(slug);
    });

    /**
     * @description
     * Awacs balancer must not have any direct access. All access must be inherited from the namespace.
     *
     * ## Reason
     * It is more convenient manage all access through Awacs namespace.
     *
     * ## Solution
     * 1) Go to the awacs namespace.
     * 2) Select the target top-level balancer from list.
     * 3) Tap "Edit" button in left sidebar.
     * 4) Choose "Auth" tab.
     * 5) Remove all personal logins and ABC groups from the balancer access.
     *
     * ## Autofix
     * Autofix is available for this rule.
     */
    check('AWACS_BALANCER_NO_DIRECT_ACCESS', async () => {
        const responses = await Promise.all(
            namespaceIds.map((namespaceId) => awacsProvider.listBalancers({namespaceId}))
        );
        const balancers = responses.flatMap((response) => response.balancers);
        assert(balancers.length !== 0, 'Awacs balancers not found');

        const balancersWithDirectAccess = balancers.filter((balancer) => {
            const logins = balancer?.meta?.auth?.staff?.owners.logins || [];
            const groupIds = balancer?.meta?.auth?.staff?.owners.groupIds || [];
            return logins.length !== 0 || groupIds.length !== 0;
        });

        if (config.autofix) {
            await fixAwacsBalancerNoDirectAccess(balancersWithDirectAccess);
            return;
        }

        assert(
            balancersWithDirectAccess.length === 0,
            `Balancer must not have any direct access. Direct access found in: ${balancersWithDirectAccess.map(({meta: {id}}) => id).join(', ')}`
        );
    });

    /**
     * @description
     * Awacs upstream must not have any direct access. All access must be inherited from the namespace.
     *
     * ## Reason
     * It is more convenient manage all access through Awacs namespace.
     *
     * ## Solution
     * 1) Go to the awacs namespace upstreams.
     * 2) Select the target upstream from the list.
     * 3) Tap "Edit" button in left sidebar.
     * 4) Choose "Auth" tab.
     * 5) Remove all personal logins and ABC groups from the upstream access.
     *
     * ## Autofix
     * Autofix is available for this rule.
     */
    check('AWACS_UPSTREAM_NO_DIRECT_ACCESS', async () => {
        const responses = await Promise.all(
            namespaceIds.map((namespaceId) => awacsProvider.listUpstreams({namespaceId}))
        );
        const upstreams = responses.flatMap((response) => response.upstreams);
        assert(upstreams.length !== 0, 'Awacs upstreams not found');

        const upstreamsWithDirectAccess = upstreams.filter((upstream) => {
            const logins = upstream?.meta?.auth?.staff?.owners.logins || [];
            const groupIds = upstream?.meta?.auth?.staff?.owners.groupIds || [];
            return logins.length !== 0 || groupIds.length !== 0;
        });

        if (config.autofix) {
            await fixAwacsUpstreamNoDirectAccess(upstreamsWithDirectAccess);
            return;
        }

        assert(
            upstreamsWithDirectAccess.length === 0,
            `Upstream must not have any direct access. Direct access found in: ${upstreamsWithDirectAccess.map(({meta: {id}}) => id).join(', ')}`
        );
    });

    /**
     * @description
     * Awacs namespace must have access only for duty service, no personal access.
     *
     * ## Reason
     * Dutywork group must have access for all namespaces for solving problems if it is needed.
     *
     * ## Solution
     * 1) Go to the your awacs namespace.
     * 2) Tap "Edit namespace" button in left sidebar.
     * 3) Remove all personal access from namespace.
     * 4) Add ["DevOps (Инфраструктура фронтенда геосервисов)"](https://abc.yandex-team.ru/services/maps-front-infra/?scope=devops) group to namespace access list in "Owners" section.
     * 5) Also add "DevOps" group from your ABC service.
     *
     * ## Autofix
     * Autofix is available for this rule.
     */
    check('AWACS_NAMESPACE_ONLY_DEVOPS_ACCESS', async () => {
        assert(devopsGroupsIds.length === 2, 'Invalid number of target DevOps groups');

        const responses = await Promise.all(namespaceIds.map((id) => awacsProvider.getNamespace({id})));
        const namespaces = responses.map((response) => response.namespace);
        assert(namespaces.length !== 0, 'Awacs namespaces not found');

        const namespacesWithNotOnlyDevopsAccess = namespaces.filter((namespace) => {
            const logins = namespace?.meta?.auth?.staff?.owners.logins || [];
            const groupIds = namespace?.meta?.auth?.staff?.owners.groupIds || [];

            return logins.length !== 0 || !isEqual(devopsGroupsIds, groupIds.sort());
        });

        if (config.autofix) {
            await fixAwacsNamespaceOnlyDevopsAccess(namespacesWithNotOnlyDevopsAccess, devopsGroupsIds);
            return;
        }

        assert(
            namespacesWithNotOnlyDevopsAccess.length === 0,
            `Namespace must have access only for duty service. Incorrect access found in: ${namespacesWithNotOnlyDevopsAccess.map(({meta: {id}}) => id).join(', ')}`
        );
    });
});
