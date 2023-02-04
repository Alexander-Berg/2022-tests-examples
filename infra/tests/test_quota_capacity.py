import logging
import yaml

from library.python import resource as rs
from collections import namedtuple

yaml_loader = getattr(yaml, 'CSafeLoader', yaml.SafeLoader)
Resource = namedtuple('Resource', [
    'vcpu', 'memory',
    'capacity_hdd', 'disk_bandwidth_hdd',
    'capacity_ssd', 'disk_bandwidth_ssd'])


def test_quota_capacity(request, account_id, gc_max_object_age_minutes):
    resources = []
    for name, data in rs.iteritems('/stage'):
        resource = _parse(data)
        logging.info('%s resource usage: %s', name, resource)
        resources.append(resource)
    logging.info('total resource usage: %s', _merge_resources(resources))

    # TODO: fetch it from target cluster?
    requested_quota = {
        'vcpu': 12000,  # 12c
        'memory': 10737418240,  # 10G
        'capacity_hdd': 1099511627776,  # 1T
        'disk_bandwidth_hdd': 3145728,  # 3M/s
        'capacity_ssd': 1099511627776,  # 1T
        'disk_bandwidth_ssd': 3145728  # 3M/s
    }

    run_freq = 30
    test_split_factor = request.config.option.modulo
    def approx_quota_limit(value, factor=1.5):
        return factor * value * test_split_factor * (gc_max_object_age_minutes / run_freq)

    # well... this is pretty lame estimation.
    # better way is to look for split_factor consumers which
    # have max sum consumption (and assume some good metric)
    max_resource_vector_dict = _merge_resources(resources, max)._asdict()
    for key, quota in requested_quota.items():
        requested_quota = approx_quota_limit(max_resource_vector_dict[key])
        assert requested_quota <= quota, \
            f"Requested resource {key} could overwhelm allowed quota ({requested_quota} > {quota}). " \
            f"Make sure that this is ok and re-write this check or consider enlarging quota for account {account_id}."

    quota_order = {}
    for key, quota in max_resource_vector_dict.items():
        quota_order[key] = approx_quota_limit(quota, 2.)
    logging.info("Quota order: %s", quota_order)


def _parse(data):
    stage_yaml = data.decode("utf-8") % {'yp_cluster': 'fake_cluster'}
    stage_dict = yaml.load(stage_yaml, Loader=yaml_loader)
    resources = list(_find_resources(stage_dict))
    return _merge_resources(resources)


def _merge_resources(resources, f=sum):
    return Resource(
        f([r.vcpu for r in resources]),
        f([r.memory for r in resources]),
        f([r.capacity_hdd for r in resources]),
        f([r.disk_bandwidth_hdd for r in resources]),
        f([r.capacity_ssd for r in resources]),
        f([r.disk_bandwidth_ssd for r in resources]),
    )


def _process_replica_sets(stage_dict, function, *args, **kwargs):
    for du_id, du_spec in stage_dict.get('spec', {}).get('deploy_units', {}).items():
        yield function(du_id, du_spec.get('multi_cluster_replica_set', {}).get('replica_set', {}), *args, **kwargs)
        yield function(du_id, du_spec.get('replica_set', {}).get('replica_set_template', {}), *args, **kwargs)


def _find_resources(stage_dict):
    default_bandwith = {
        'hdd': 1048576,  # 1M/s
        'ssd': 1048576  # 1M/s
    }
    def _patch_resources_and_volumes(du_id, replica_set_dict):
        pod_spec = replica_set_dict.get('pod_template_spec', {}).get('spec', {})
        for volume_request in pod_spec.get('disk_volume_requests', []):
            quota_policy = volume_request.get('quota_policy', {})
            storage_type = volume_request.get('storage_class', 'hdd')
            yield Resource(**{
                'vcpu': 0,
                'memory': 0,
                'capacity_hdd': 0,
                'disk_bandwidth_hdd': default_bandwith['hdd'],
                'capacity_ssd': 0,
                'disk_bandwidth_ssd':  default_bandwith['ssd'],
                'disk_bandwidth_' + storage_type: quota_policy.get('bandwidth_limit', default_bandwith[storage_type]),
                'capacity_' + storage_type: quota_policy['capacity']
            })

        resource_requests = pod_spec.get('resource_requests', {})
        yield Resource(resource_requests.get('vcpu_limit', 0),
                       resource_requests.get('memory_limit', 0),
                       0, 0, 0, 0)

    for resource in _process_replica_sets(stage_dict, _patch_resources_and_volumes):
        for r in resource:
            yield r
