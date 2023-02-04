import {isEqual} from 'lodash';
import {config, externalSlb} from '../../config';
import assert from '../../utils/assert';
import awacsProvider, {awacsApi} from '../../providers/awacs';
import {getNamespaceId} from '../../utils/awacs';

const XFFY_HEADER: awacsApi.awacs.modules.L7Macro.IHeaderAction = {
    create: {keepExisting: false, target: 'X-Forwarded-For-Y', func: 'realip'}
};

const ORIGINAL_HEADERS_EXT: awacsApi.awacs.modules.L7Macro.IHeaderAction[] = [
    {create: {keepExisting: false, target: 'X-Forwarded-For', func: 'realip'}},
    {create: {keepExisting: false, target: 'X-Forwarded-Host', func: 'host'}},
    {create: {keepExisting: false, target: 'X-Forwarded-Proto', func: 'scheme'}},
    {create: {keepExisting: false, target: 'X-Real-IP', func: 'realip'}},
    {create: {keepExisting: false, target: 'X-Req-Id', func: 'reqid'}},
    {create: {keepExisting: false, target: 'X-Source-Port-Y', func: 'realport'}}
];

const ORIGINAL_HEADERS_INT: awacsApi.awacs.modules.L7Macro.IHeaderAction[] = [
    {create: {keepExisting: true, target: 'X-Forwarded-For', func: 'realip'}},
    {create: {keepExisting: true, target: 'X-Forwarded-Host', func: 'host'}},
    {create: {keepExisting: true, target: 'X-Forwarded-Proto', func: 'scheme'}},
    {create: {keepExisting: true, target: 'X-Real-IP', func: 'realip'}},
    {create: {keepExisting: true, target: 'X-Req-Id', func: 'reqid'}},
    {create: {keepExisting: true, target: 'X-Source-Port-Y', func: 'realport'}}
];

config.runForServices('awacs/top-level', ({slug, check}) => {
    const namespaceId = getNamespaceId(slug);
    let balancers: awacsApi.awacs.model.IBalancer[];

    before(async () => {
        balancers = (await awacsProvider.listBalancers({namespaceId})).balancers;
    });

    check('AWACS_CONFIG_EXISTS', async () => {
        assert(balancers.length > 1, `Can\'t get config for namespaceId ${namespaceId}`);
    });

    /**
     * @description
     *
     * ## Reason
     * All our services should have the necessary minimum of standard headers
     *
     * ## Solution
     * For **External balancer** it should look something like this in top_level config:
     * ```yaml
        headers:
          - create: {target: X-Forwarded-For, func: realip}
          - create: {target: X-Forwarded-Host, func: host}
          - create: {target: X-Forwarded-Proto, func: scheme}
          - create: {target: X-Real-IP, func: realip}
          - create: {target: X-Req-Id, func: reqid}
          - create: {target: X-Source-Port-Y, func: realport}
     * ```
     *
     * For **Internal balancer** it should look something like this:
     * ```yaml
        headers:
          - create: {target: X-Forwarded-For, keep_existing: true, func: realip}
          - create: {target: X-Forwarded-Host, keep_existing: true, func: host}
          - create: {target: X-Forwarded-Proto, keep_existing: true, func: scheme}
          - create: {target: X-Real-IP, keep_existing: true, func: realip}
          - create: {target: X-Req-Id, keep_existing: true, func: reqid}
          - create: {target: X-Source-Port-Y, keep_existing: true, func: realport}
     * ```
     *
     * 1. Go To [AWACS](https://nanny.yandex-team.ru/ui/#/awacs/)
     * 1. Select your balancer
     * 1. Open "top_level" config
     * 1. Check the configuration of the section
     *
     * {% note info %}
     *
     * * **External balancer** - these are load balancers that external users go to directly
     * * **Internal balancer** - these are balancers that either other Yandex services go to, or strictly internal Yandex users
     *
     * If you don't know which balancer you have, check with the [duty DevOps](https://abc.yandex-team.ru/services/maps-front/?scope=dutywork)
     *
     * {% endnote %}
     */
    check('AWACS_TOP_LEVEL_HEADERS_EXIST', async () => {
        const originalHeaders = externalSlb.includes(namespaceId) ? ORIGINAL_HEADERS_EXT : ORIGINAL_HEADERS_INT;

        const wrongBalancers = [];
        for (const balancer of balancers) {
            const currentHeaders = balancer.spec.yandexBalancer.config.l7Macro.headers;

            const withoutSomeHeader = originalHeaders.some(
                (originalHeader) => !hasHeader(currentHeaders, originalHeader)
            );
            if (withoutSomeHeader) {
                wrongBalancers.push(getSlbName(balancer));
            }
        }

        assert(wrongBalancers.length < 1, `Incorrect top-level headers. DC: ${wrongBalancers.join(', ')}`);
    });

    /**
     * @description
     * ## Reason
     * You need to use l7_macro version 0.3.x and higher in top_level config.
     *
     * ## Solution
     * For **External balancer** it should look something like this:
     * ```yaml
       l7_macro:
         version: 0.3.10
         core: {}
     * ```
     *
     * For **Internal balancer** it should look something like this:
     * ```yaml
        l7_macro:
          version: 0.3.10
          core:
            trust_x_forwarded_for_y: true
     * ```
     *
     * 1. Go To [AWACS](https://nanny.yandex-team.ru/ui/#/awacs/)
     * 1. Select your balancer
     * 1. Open "top_level" config
     * 1. Check the configuration of the section

     * {% note info %}
     *
     * * **External balancer** - these are load balancers that external users go to directly
     * * **Internal balancer** - these are balancers that either other Yandex services go to, or strictly internal Yandex users
     *
     * If you don't know which balancer you have, check with the [duty DevOps](https://abc.yandex-team.ru/services/maps-front/?scope=dutywork)
     *
     * {% endnote %}
     */
    check('AWACS_XFFY_HEADER', async () => {
        const wrongBalancers = [];
        for (const balancer of balancers) {
            // @ts-ignore Необходимо обновить @yandex-int/awacs-api, чтобы подтянулись новые поля.
            const xffy = balancer.spec.yandexBalancer.config.l7Macro.core?.trustXForwardedForY;
            const currentHeaders = balancer.spec.yandexBalancer.config.l7Macro.headers;
            if (externalSlb.includes(namespaceId) && (xffy || (!xffy && hasHeader(currentHeaders, XFFY_HEADER)))) {
                wrongBalancers.push(getSlbName(balancer));
            }
        }

        assert(wrongBalancers.length < 1, `Incorrect X-Forwarded-For-Y header. DC: ${wrongBalancers.join(', ')}`);
    });
});

function getSlbName(balancer: awacsApi.awacs.model.IBalancer) {
    return balancer.meta?.location?.ypCluster;
}

function hasHeader(
    headers: awacsApi.awacs.modules.L7Macro.IHeaderAction[],
    checkHeader: awacsApi.awacs.modules.L7Macro.IHeaderAction
) {
    return headers.some((header) => isEqual(header, checkHeader));
}
