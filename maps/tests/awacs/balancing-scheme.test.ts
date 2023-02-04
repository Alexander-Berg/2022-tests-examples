import {config} from '../../config';
import {getAwacsBackendsUpstreams} from '../../providers/awacs';
import deploy from '../../providers/deploy';
import assert from '../../utils/assert';
import {getNamespaceId, getUpstreamLink} from '../../utils/awacs';

const PODS_COUNT_THRESHOLD = 4;

config.runForServices('awacs/balancing-scheme', ({slug, check}) => {
    /**
     * @description
     * If a service has less than four pods in each DC then its upstreams should be configured with flat balancing scheme.
     * If a service has more than three pods in each DC then its upstreams should be configured with geo balancing scheme.
     *
     * ## How to Fix
     *
     * 1. If a service has no 'production' stage in YDeploy, please create one.
     * 2. If a service has more than three pods in each DC in production stage, please add 'by_dc_scheme' key
     * to the configuration of corresponding upstreams. And vice versa, if a service has less than four pods
     * in each DC in production stage, please add 'flat_scheme' key to the configuration of corresponding upstreams.
     */
    check('AWACS_BALANCING_SCHEME', async () => {
        const podsCount = (await deploy.getDeployUnits(slug))
            // TODO: add logic to find appropriate DU name.
            ?.app
            ?.multi_cluster_replica_set
            ?.replica_set.clusters[0]
            ?.spec
            ?.replica_count;

        assert(
            !isNaN(podsCount),
            `Could not obtain replicas count for ${slug}, please check if 'production' stage is present`
        );

        const namespaceId = getNamespaceId(slug);
        const sectionName = podsCount < PODS_COUNT_THRESHOLD ? 'flatScheme' : 'byDcScheme';
        const problemUpstream = (await getAwacsBackendsUpstreams(namespaceId)).find(
            (upstream) => !(sectionName in upstream.spec.yandexBalancer.config.l7UpstreamMacro)
        );
        const message = problemUpstream ?
            `Upstream ${problemUpstream.meta.id} has no '${sectionName}' section in its config: ${getUpstreamLink(problemUpstream)}` :
            '';

        assert(problemUpstream === undefined, message);
    });
});
