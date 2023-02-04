import {config} from '../../config';
import assert from '../../utils/assert';
import {isRobot} from '../../utils/robots';
import idm, {IdmRole, isTargetGroup} from '../../providers/idm';

config.runForServices('deploy/access', ({slug, check}) => {
    let roles: IdmRole[];

    before(async () => {
        const idmRoles = await idm.getRoles({
            system: 'deploy-prod',
            path: `/${slug}/`
        });
        roles = idmRoles.objects.filter((role) => Boolean(role.group || role.user));
    });

    /**
     * @description
     * Prohibits personal accesses.
     *
     * ## Rationale
     * Personal access to internal services is considered harmful.  If a crucial access right is assigned only to a person it will be rendered read-only when that person takes their annual leave or gets sick. That's why only access through ABC roles are allowed.
     *
     * ## Solution
     * 1. Go to your deploy project.
     * 1. Tap "Manage Roles" link in "General access" section.
     * 1. You will be moved to IDM.
     * 1. Remove all personal access from the presented list.
     */
    check('DEPLOY_ONLY_ABC_ACCESS', async () => {
        const usersWithAccess = roles
            .filter((role) => role.user && !isRobot(role.user.username))
            .map(({user}) => user.username);
        assert(
            usersWithAccess.length === 0,
            `Only ABC groups can access the application (${usersWithAccess.join(', ')})`
        );
    });

    /**
     * @description
     * Requires access for duty DevOps team.
     *
     * ## Rationale
     * We have a special DevOps team which watches closely for all our services at nights and holidays. To fix incidents they should have an extra permissions for all services. That's why not only maintainers but DevOps team must have the same accesses.
     *
     * ## Solution
     * 1. Go to your deploy project.
     * 1. Tap "Manage Roles" link in "General access" section.
     * 1. You will be moved to IDM.
     * 1. Tap "Запросить роль" button in header.
     * 1. Fill in first field with value `Я.Деплой > YOUR_ABC_SLUG > OWNER`.
     * 1. Fill in second field with value `maps-front-infra_devops` of group ["DevOps (Инфраструктура фронтенда геосервисов)"](https://abc.yandex-team.ru/services/maps-front-infra/?scope=devops).
     */
    check('DEPLOY_DEVOPS_ACCESS', async () => {
        assert(
            roles.some((role) => role.group && isTargetGroup(role.group, {
                system: 'svc',
                slug: 'maps-front-infra',
                role: 'devops'
            })),
            'Application must have access for duty service'
        );
    });
});
