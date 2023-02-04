import {awacs} from '@yandex-int/awacs-api';
import semver from 'semver';
import {config} from '../../config';
import assert from '../../utils/assert';
import awacsProvider, {getAwacsBackendsUpstreams, getNamespacesIds} from '../../providers/awacs';
import {getNamespaceId} from '../../utils/awacs';
import {roundNumber} from '../../utils/common';

const REQUIRED_UPSTREAMS = ['main', 'default'];

config.runForServices('awacs/upstream', ({slug, check}) => {
    const namespaceId = getNamespaceId(slug);
    let upstreamsWithBackends: awacs.model.IUpstream[];

    before(async () => {
        try {
            upstreamsWithBackends = await getAwacsBackendsUpstreams(namespaceId);
        } catch (e) {
            assert(false, 'Cannot get upstreams');
        }
    });

    /**
     * @description
     * Awacs balancer should have specific list of upstreams.
     *
     * ## Reason
     * For unification and separation of concerns.
     *
     * ## Solution
     * 1) Go to your awacs namespace.
     * 2) Go to upstreams list.
     * 3) Check actual upstreams list with required, witch contains: `main, default`.
     *
     * {% note info %}
     *
     * Each type of upstream designed for a specific purpose:
     * - `main` - uses for base logic of your balancer
     * - `default` - serves as a handler in case the previous upstreams did not process the request. It should be latest in upstreams order and have field `matcher.any: true`. Example config:
     *     ```yml
     *     l7_upstream_macro:
     *         version: 0.0.1
     *         id: default
     *         matcher:
     *             any: true
     *         static_response:
     *             status: 421
     *             content: Misdirected Request
     *     ```
     *
     * {% endnote %}
     */
    check('AWACS_UPSTREAMS_EXISTS', async () => {
        const namespacesIds = await getNamespacesIds(slug);

        const asserts: string[] = [];

        if (namespacesIds.length === 0) {
            asserts.push('There are no namespaces');
        }

        await Promise.all(namespacesIds.map(async (namespaceId) => {
            const upstreamsList = (await awacsProvider.listUpstreams({namespaceId})).upstreams;
            const missingUpstreams = REQUIRED_UPSTREAMS.filter(
                (upstreamName) => !upstreamsList.map((upstream) => upstream.meta?.id).includes(upstreamName)
            );

            if (upstreamsList.length === 0) {
                asserts.push(`[${namespaceId}]: There are no upstreams`);
            }

            if (missingUpstreams.length !== 0) {
                asserts.push(`[${namespaceId}]: There should be ${missingUpstreams.join(', ')} ${missingUpstreams.length > 1 ? 'upstreams' : 'upstream'}`);
            }
        }));

        assert(asserts.length === 0, asserts.join('\n'));
    });

    /**
     * @description
     * Awacs balancer's upstream should have reattempts share section.
     *
     * ## Reason
     * Reattemps during major network issues can take down instances so they should be limited.
     *
     * ## Solution
     * 1) Go to your awacs namespace.
     * 2) Go to upstreams list.
     * 3) Choose the "main" upstream.
     * 4) Add health check to upstream yaml config in balancer section e.g.:
     * ```yml
     * l7_upstream_macro:
     *     flat_scheme:
     *         balancer:
     *             max_reattempts_share: 0.2
     * ```
     *
     * {% note info %}
     *
     * More information you can find on [Wiki](https://wiki.yandex-team.ru/awacs/upstream-easy-mode/#balancer)
     *
     * {% endnote %}
     */
    check('AWACS_UPSTREAMS_EXISTS_REATTEMPTS_SHARE', async () => {
        const upstreamsToCheck = upstreamsWithBackends;
        const failedUpstreamIds = upstreamsToCheck.filter((upstream) => {
            const maxReattemptsShare = roundNumber(Number(
                getUpstreamScheme(upstream).balancer.maxReattemptsShare
            ));
            return isNaN(maxReattemptsShare) || maxReattemptsShare > 0.2;
        }).map((upstream) => upstream.meta.id);

        assert(
            failedUpstreamIds.length === 0,
            `max_reattempts_share shouldn't be greater than 0.2 for upstreams with ids: ${failedUpstreamIds.join(', ')}`
        );
    });

    /**
     * @description
     * Awacs balancer's upstream should have pessimized endpoints share section.
     *
     * ## Reason
     * Awacs will ignore health checks if the share of unavailable endpoints greater than this value.
     * It should be 1.0, but the maximum value allowed by awacs is 0.5.
     *
     * ## Solution
     * 1) Go to your awacs namespace.
     * 2) Go to upstreams list.
     * 3) Choose the "main" upstream.
     * 4) Add health check to upstream yaml config in balancer section e.g.:
     * ```yml
     * l7_upstream_macro:
     *     flat_scheme:
     *         balancer:
     *             max_pessimized_endpoints_share: 0.5
     * ```
     *
     * {% note info %}
     *
     * More information you can find on [Wiki](https://wiki.yandex-team.ru/awacs/upstream-easy-mode/#balancer)
     *
     * {% endnote %}
     */
    check('AWACS_UPSTREAMS_EXISTS_PESSIMIZED_ENDPOINTS_SHARE', async () => {
        const upstreamsToCheck = upstreamsWithBackends;
        const failedUpstreamIds = upstreamsToCheck.filter((upstream) => {
            const maxPessimizedEndpointsShare = roundNumber(Number(
                getUpstreamScheme(upstream).balancer.maxPessimizedEndpointsShare
            ));
            return isNaN(maxPessimizedEndpointsShare) || maxPessimizedEndpointsShare !== 0.5;
        }).map((upstream) => upstream.meta.id);

        assert(
            failedUpstreamIds.length === 0,
            `max_pessimized_endpoints_share should be greater equal to 0.5 for upstreams with ids: ${failedUpstreamIds.join(', ')}`
        );
    });

    /**
     * @description
     * Awacs balancer's upstream should have health check section.
     *
     * ## Reason
     * Health check section difines how balancer can identify if endpoint alive or not.
     *
     * ## Solution
     * 1) Go to your awacs namespace.
     * 2) Go to upstreams list.
     * 3) Choose the "main" upstream.
     * 4) Add health check to upstream yaml config in balancer section for example.slb.maps.yandex.net namespace e.g.:
     * ```yml
     * l7_upstream_macro:
     *     flat_scheme:
     *         balancer:
     *             health_check:
     *             delay: 5s
     *             request: >-
     *                 GET /ping HTTP/1.1\nHost:
     *                 example.slb.maps.yandex.net\nUser-agent: l7-balancer\n\n
     * ```
     *
     * {% note info %}
     *
     * More information you can find on [Wiki](https://wiki.yandex-team.ru/awacs/upstream-easy-mode/#balancer)
     *
     * {% endnote %}
     */
    check('AWACS_UPSTREAMS_HEALTH_CHECK', async () => {
        const upstreamsToCheck = upstreamsWithBackends;
        const failedUpstreamIds = upstreamsToCheck.filter((upstream) => {
            const healthCheck = getUpstreamScheme(upstream).balancer.healthCheck;
            return healthCheck?.delay !== '5s' || !healthCheck?.request.includes('/ping');
        }).map((upstream) => upstream.meta.id);

        assert(
            failedUpstreamIds.length === 0,
            `health_check section should exists for upstreams with ids: ${failedUpstreamIds.join(', ')}`
        );
    });

    /**
     * @description
     * Awacs balancer's upstream must have disabled nonidempotent retries.
     *
     * ## Reason
     * Nonidempotent reties can cause unpredicted behaviors.
     * By default they are enabled in awacs, so you should explicitly disable it.
     *
     * ## Solution
     * Add `retry_non_idempotent: false` to upstream yaml config in balancer section e.g.:
     * ```yml
     * l7_upstream_macro:
     *     flat_scheme:
     *         balancer:
     *             retry_non_idempotent: false
     * ```
     */
    check('AWACS_UPSTREAM_RETRY_NON_IDEMPOTENT_DISABLED', async () => {
        const upstreamsToCheck = upstreamsWithBackends;
        const failedUpstreamIds = upstreamsToCheck.filter((upstream) => {
            const retryNonIdempotent = getUpstreamScheme(upstream).balancer.retryNonIdempotent;
            return retryNonIdempotent !== false;
        }).map((upstream) => upstream.meta.id);
        assert(
            failedUpstreamIds.length === 0,
            `retry_non_idempotent should be explicitly set to false for upstreams with ids: ${failedUpstreamIds.join(', ')}`
        );
    });

    /**
     * @description
     * Awacs balancer's upstream must have exactly 2 attempts.
     *
     * ## Reason
     * 1 retry can protect from minor network issues/flaps.
     * However additional retries are less effective, but they generate more rps and encrease timings.
     *
     * ## Solution
     * Add `attempts: 2` to upstream yaml config in balancer section e.g.:
     * ```yml
     * l7_upstream_macro:
     *     flat_scheme:
     *         balancer:
     *             attempts: 2
     * ```
     *
     * Or:
     * ```yml
     * l7_upstream_macro:
     *     flat_scheme:
     *         balancer:
     *             attempts: 1
     *             fast_attempts: 2
     * ```
     */
    check('AWACS_UPSTREAMS_RETRIES', async () => {
        const upstreamsToCheck = upstreamsWithBackends;
        const failedUpstreamIds = upstreamsToCheck.filter((upstream) => {
            const {attempts, fastAttempts} = getUpstreamScheme(upstream).balancer;
            return attempts !== 2 && !(attempts === 1 && fastAttempts === 2);
        }).map((upstream) => upstream.meta.id);
        assert(
            failedUpstreamIds.length === 0,
            `Upstreams with following ids should have 2 attempts: ${failedUpstreamIds.join(', ')}`
        );
    });

    /**
     * @description
     * Awacs balancer's L7 upstream macro version must be at least 0.2.3
     *
     * ## Reason
     * This is the last recommended version by Awacs team.
     *
     * ## Solution
     * Set `l7_upstream_macro.version` parameter to "0.2.3".
     *
     * ```yml
     * l7_upstream_macro:
     *     version: 0.2.3
     * ```
     */
    check('AWACS_UPSTREAM_MACRO_VERSION', async () => {
        const MIN_UPSTREAM_MACRO_VERSION = '0.2.3';
        const upstreamsToCheck = upstreamsWithBackends;

        const failedUpstreamIds = upstreamsToCheck.filter((upstream) => {
            const currentVersion = upstream?.spec?.yandexBalancer?.config?.l7UpstreamMacro?.version;
            return currentVersion && semver.lt(currentVersion, MIN_UPSTREAM_MACRO_VERSION);
        }).map((upstream) => upstream.meta.id);

        assert(
            failedUpstreamIds.length === 0,
            `l7_upstream_macro version must be at least ${MIN_UPSTREAM_MACRO_VERSION}: ${failedUpstreamIds.join(', ')}`
        );
    });
});

function getUpstreamScheme(upstream) {
    return upstream?.spec?.yandexBalancer?.config.l7UpstreamMacro.byDcScheme ||
        upstream?.spec?.yandexBalancer?.config.l7UpstreamMacro.flatScheme;
}
