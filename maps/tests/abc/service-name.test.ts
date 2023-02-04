import {config} from '../../config';
import assert from '../../utils/assert';

config.runForServices('abc/service-name', ({slug, check}) => {
    /**
     * @description
     * Requires `maps-front-` prefix for ABC service slug.
     *
     * ## Rationale
     * Prefix `maps-front` is used to separate our team services from others. The same prefix is used in various internal services: ABC, Awacs, and etc. It makes it possible to connect all this entities between different systems.
     *
     * ## Solution
     * ABC slug can't be changed, so you should create a new one:
     * 1. Rename current ABC service by adding "[OLD]" prefix to its name.
     * 1. Create a new ABC service with a correct slug.
     * 1. Copy all collaborators from the old service to the new one.
     * 1. Move all contacts from the old ABC service to the new.
     * 1. Change ABC service in the ACL of all projects (Deploy, AWACS, etc).
     * 1. Transfer all firewall rules in [Puncher](https://puncher.yandex-team.ru/).
     * 1. Remove old ABC service.
     */
    check('ABC_SERVICE_NAME_PREFIX', () => {
        assert(slug.match(/^maps-front-/), 'ABC slug should start with maps-front- prefix');
    });

    /**
     * @description
     * Requires only lowercase alphanumeric characteds, the dash and dot symbols for ABC service name.
     *
     * ## Rationale
     * [{#T}](./ABC_SERVICE_NAME_PREFIX.md#Rationale)
     *
     * ## Solution
     * [{#T}](./ABC_SERVICE_NAME_PREFIX.md#Solution)
     */
    check('ABC_SERVICE_NAME_CHARACTERS', () => {
        assert(
            !slug.match(/[^a-z0-9-\.]/),
            'ABC slug should contain only lowercase alphanumeric characteds and the dash and dot symbols'
        );
    });

    /**
     * @description
     * Requires ABS slug ending with an alphanumeric character.
     *
     * ## Rationale
     * [{#T}](./ABC_SERVICE_NAME_PREFIX.md#Rationale)
     *
     * ## Solution
     * [{#T}](./ABC_SERVICE_NAME_PREFIX.md#Solution)
     */
    check('ABC_SERVICE_NAME_END', () => {
        assert(slug.match(/[a-z0-9]+$/), 'ABS slug should end with an alphanumeric character');
    });
});
