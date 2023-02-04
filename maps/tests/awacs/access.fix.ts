import {awacs} from '@yandex-int/awacs-api';
import {diff} from 'jsondiffpatch';
import {cloneDeep, zip} from 'lodash';
import {config} from '../../config';
import awacsProvider from '../../providers/awacs';
import {DryRunError} from '../../utils/autofix';

export async function fixAwacsBalancerNoDirectAccess(balancers: awacs.model.IBalancer[]) {
    if (balancers.length === 0) {
        return;
    }

    const newBalancers = balancers.map((balancer) => {
        const newBalancer = cloneDeep(balancer);
        newBalancer.meta.auth.staff.owners.logins = [];
        newBalancer.meta.auth.staff.owners.groupIds = [];
        return newBalancer;
    });

    if (config.autofix.dryRun) {
        const changes = zip(balancers, newBalancers).map(([balancer, newBalancer]) => ({
            message: balancer.meta.id,
            diff: diff(balancer, newBalancer)
        }));

        throw new DryRunError(changes);
    }

    await Promise.all(newBalancers.map((newBalancer) => awacsProvider.updateBalancer(newBalancer)));
}

export async function fixAwacsUpstreamNoDirectAccess(upstreams: awacs.model.IUpstream[]) {
    if (upstreams.length === 0) {
        return;
    }

    const newUpstreams = upstreams.map((upstream) => {
        const newUpstream = cloneDeep(upstream);
        newUpstream.meta.auth.staff.owners.logins = [];
        newUpstream.meta.auth.staff.owners.groupIds = [];
        return newUpstream;
    });

    if (config.autofix.dryRun) {
        const changes = zip(upstreams, newUpstreams).map(([upstream, newUpstream]) => ({
            message: upstream.meta.id,
            diff: diff(upstream, newUpstream)
        }));

        throw new DryRunError(changes);
    }

    await Promise.all(newUpstreams.map((newUpstream) => awacsProvider.updateUpstream(newUpstream)));
}

export async function fixAwacsNamespaceOnlyDevopsAccess(
    namespaces: awacs.model.INamespace[],
    devopsGroupsIds: string[]
) {
    if (namespaces.length === 0) {
        return;
    }

    const newNamespaces = namespaces.map((namespace) => {
        const newNamespace = cloneDeep(namespace);
        newNamespace.meta.auth.staff.owners.logins = [];
        newNamespace.meta.auth.staff.owners.groupIds = [...devopsGroupsIds];
        return newNamespace;
    });

    if (config.autofix.dryRun) {
        const changes = zip(namespaces, newNamespaces).map(([ns, newNs]) => ({
            message: ns.meta.id,
            diff: diff(ns, newNs)
        }));

        throw new DryRunError(changes);
    }

    await Promise.all(newNamespaces.map((newNs) => awacsProvider.updateNamespace(newNs)));
}
