import os
import codecs
from collections import defaultdict
from yatest.common import binary_path, canonical_file, test_output_path

from balancer.production.x.extract_backends_and_patterns.lib import get_modules_backends


_KNOSS_LOCATION_SWITCH = '/knoss_locations'
_PROD_LOCATION_SWITCH = '/production.weights'


def get_config():
    configs = os.listdir(binary_path('balancer/config/build'))
    if len(configs) != 1:
        raise Exception('Expected one config, got {}'.format(configs))
    config_file_path = binary_path('balancer/config/build/{cfg}/{cfg}.cfg'.format(cfg=configs[0]))
    with codecs.open(config_file_path, encoding='utf-8') as f:
        config = f.read()
    return config


class Backend:
    """backend description, e.g. name='backends_search_123' exp_id=12345"""
    def __init__(self, name, exp_id):
        self.name = name
        self.exp_id = exp_id

    def __repr__(self):
        return "Backend(name='{}', exp_id={})".format(self.name, self.exp_id)


class ConfigExtractor:
    def __init__(self, config):
        extracted = get_modules_backends(config, dump_tree=True)
        self.tree = extracted['tree']

    def _process_sd(self, node, exp_id):
        return [Backend('backends_{}#{}'.format(backend['cluster_name'], backend['endpoint_set_id']), exp_id) for backend in node['endpoint_sets']]

    def _location_backends(self, node, exp_id):
        backends = list()
        if isinstance(node, dict):
            if 'backends' in node:
                backends.append(Backend(node['backends'], exp_id))
            for k, v in node.iteritems():
                if k == 'sd':
                    backends += self._process_sd(v, exp_id)
                else:
                    backends += self._location_backends(v, exp_id)
        elif isinstance(node, list):
            for v in node:
                backends += self._location_backends(v, exp_id)
        return backends

    def _process_service_locations(self, node, exp_id):
        locations = defaultdict(list)

        for k, v in node.iteritems():
            if isinstance(v, dict):
                locations[k].extend(self._location_backends(v, exp_id))

        return locations

    def _parse_exp_id(self, node):
        assert "match_fsm" in node, "ConfigExtractor: there is no match_fsm, cant find exp_id"
        match_fsm = node["match_fsm"]
        assert "header" in match_fsm, "ConfigExtractor: exp_id should be in header, but header not found"
        header = match_fsm["header"]
        assert header["name"] in ("Y-Balancer-Experiments", "X-Yandex-ExpBoxes"), "ConfigExtractor: exp_id should be in Y-Balancer-Experements, but name = %s" % header["name"]
        return int(header["value"][2:-2])

    def _service_locations(self, node, service_name, node_type, weights_file_name, current_node_name=None, exp_id=None, in_service=False, in_service_node=False):
        """ return dict, e.g {'search_man':[Backend('backends_search_123', 12345)]} """
        locations = defaultdict(list)
        if current_node_name is not None and current_node_name.startswith("exp_backends_"):
            assert exp_id is None, "does config try to match experiment inside experiment?"
            exp_id = self._parse_exp_id(node)

        if isinstance(node, dict):
            if in_service and node.get('weights_file', '').endswith(weights_file_name):
                return self._process_service_locations(node, exp_id)
            for k, v in node.iteritems():
                if in_service_node and k != service_name:
                    continue
                is_service = in_service or in_service_node
                is_service_node = k == node_type
                for loc, backends in self._service_locations(v, service_name, node_type, weights_file_name, k, exp_id, is_service, is_service_node).iteritems():
                    locations[loc].extend(backends)
        elif isinstance(node, list):
            for v in node:
                for loc, backends in self._service_locations(v, service_name, node_type, weights_file_name, current_node_name, exp_id, in_service).iteritems():
                    locations[loc].extend(backends)
        return locations

    def _term_locations(self, node, service_name, node_type, weights_file_name):
        if isinstance(node, dict):
            for k, v in node.iteritems():
                if k == 'regexp' and isinstance(v, dict) and 'trusted_networks' in v and 'default' in v:
                    return self._service_locations(v['default'], service_name, node_type, weights_file_name)

                ret = self._term_locations(v, service_name, node_type, weights_file_name)
                if ret:
                    return ret

        elif isinstance(node, list):
            for v in node:
                ret = self._term_locations(v, service_name, node_type, weights_file_name)
                if ret:
                    return ret

    def term_service_locations(self, service_name):
        return self._term_locations(self.tree, service_name, 'regexp_path', _KNOSS_LOCATION_SWITCH)

    def knoss_service_locations(self, service_name):
        return self._service_locations(self.tree, service_name, 'regexp_path', _PROD_LOCATION_SWITCH)

    def _one_enabled(self, locations, control_file):
        cases = []
        if locations is None:
            return []
        for location in locations:
            exp_ids = {backend.exp_id for backend in locations[location]}
            for exp_id in exp_ids:
                case = {l: -1 for l in locations}
                case[location] = 1

                backend_names = [backend.name for backend in locations[location] if backend.exp_id == exp_id]

                if backend_names:
                    one_case = {'weights': case,
                                'backends': backend_names,
                                'control_file': control_file[1:],
                                'location': location}
                    if exp_id is not None:
                        one_case["exp_id"] = exp_id

                    cases.append(one_case)
        return cases

    def one_enabled_term(self, service_name):
        return self._one_enabled(self.term_service_locations(service_name), _KNOSS_LOCATION_SWITCH)

    def one_enabled_knoss(self, service_name):
        return self._one_enabled(self.knoss_service_locations(service_name), _PROD_LOCATION_SWITCH)


def one_enabled_location_knoss_only(service_name):
    result = {'argvalues': ConfigExtractor(get_config()).one_enabled_term(service_name)}
    result['ids'] = [case.pop('location') for case in result['argvalues']]
    return result


def one_enabled_location_subheavy(service_name):
    result = {'argvalues': ConfigExtractor(get_config()).one_enabled_knoss(service_name)}
    result['ids'] = [case.pop('location') for case in result['argvalues']]
    return result


def locations_backends_canon_test(service_name, location_getter):
    config_extractor = ConfigExtractor(get_config())
    locations_backends_list = location_getter(config_extractor, service_name)

    path = test_output_path('{}_locations'.format(service_name))
    locations_backends = {}
    for loc in locations_backends_list:
        loc_name = loc['location']
        exp_id = loc.get('exp_id')

        if loc.get('exp_id') is not None:
            key = "{}|{}".format(loc_name, exp_id)
        else:
            key = loc_name

        locations_backends[key] = ','.join(sorted(loc['backends']))

    with open(path, 'w') as f:
        for location in sorted(locations_backends.keys()):
            f.write("{}: {}\n".format(location, locations_backends[location]))
    return canonical_file(path, local=True)


def knoss_only_locations_backends_canon_test(service_name):
    return locations_backends_canon_test(service_name, ConfigExtractor.one_enabled_term)


def subheavy_locations_backends_canon_test(service_name):
    return locations_backends_canon_test(service_name, lambda config_extractor, service_name: ConfigExtractor.one_enabled_knoss(config_extractor, service_name))
