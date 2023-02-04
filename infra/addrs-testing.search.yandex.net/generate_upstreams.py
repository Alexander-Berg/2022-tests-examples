from collections import OrderedDict
import io
import json
import yaml


PATTERN = 'upstream_{}_{}.yaml'


def ordered_dump(data, stream=None, Dumper=yaml.Dumper, **kwds):  # pylint: disable=invalid-name

    class OrderedDumper(Dumper):  # pylint: disable=too-many-ancestors
        """Missing function docstring"""
        pass

    def _dict_representer(dumper, data):
        return dumper.represent_mapping(
            yaml.resolver.BaseResolver.DEFAULT_MAPPING_TAG,
            data.items())

    OrderedDumper.add_representer(OrderedDict, _dict_representer)
    return yaml.dump(data, stream, OrderedDumper, **kwds)


def write_yaml(key, sub_key, result):
    with io.open(PATTERN.format(key, sub_key), 'w', encoding='utf-8') as ofile:
        ofile.write(six.text_type(ordered_dump(result, Dumper=yaml.SafeDumper, explicit_start=True)))


def default_upstream():
    with open('testing_configurations.json') as configfile:
        cfile = json.load(configfile)

        result = {
            'regexp_section': {
                'matcher': {},
                'modules': [
                    {'stats_eater': {}},
                    {
                        'balancer2': OrderedDict([
                            ('weighted2', {}),
                            ('unique_policy', {}),
                            ('generated_proxy_backends', OrderedDict([
                                ('proxy_options', {
                                    'connect_timeout': '100ms',
                                    'backend_timeout': '10s',
                                    'keepalive_count': 10
                                }),
                                ('gencfg_groups', [
                                    {
                                        'name': '{}'.format(group),
                                        'version': 'trunk'
                                    } for group in cfile['synonyms']['backend']['default']
                                ])
                            ]))
                        ])
                    }
                ]
            }
        }

        write_yaml('common', 'default', result)


def processing_balancer_tags(data, label):
    with open('balancer_tags.json') as configfile:
        cfile = json.load(configfile)

        for key, value in cfile.iteritems():
            for sub_key, sub_value in value.iteritems():
                label += 100
                modules = [
                    {
                        'rewrite': {
                            'actions': [
                                {
                                    'regexp': '/{}(-|/){}(/)?(.*)?'.format(key, sub_key),
                                    'rewrite': '/%3',
                                    'split': 'url'
                                }
                            ]
                        }
                    },
                    {'stats_eater': {}},
                    {
                        'balancer2': OrderedDict([
                            ('weighted2', {}),
                            ('unique_policy', {}),
                            ('generated_proxy_backends', OrderedDict([
                                ('proxy_options', {
                                    'connect_timeout': '100ms',
                                    'backend_timeout': '10s',
                                    'keepalive_count': 10
                                }),
                                (
                                    'gencfg_groups',
                                    [
                                        {'name': group, 'version': 'trunk'} for group in sub_value
                                    ]
                                )
                            ]))
                        ])
                    }
                ]

                result = {
                    'regexp_section': {
                        'matcher': {'match_fsm': {'url': '/{}(-|/){}((/|\\\\?).*)?'.format(key, sub_key)}},
                        'modules': modules
                    }
                }

                write_yaml(key, sub_key, result)

                if data.get(PATTERN.format(key, sub_key)) is None:
                    data[PATTERN.format(key, sub_key)] = {'order': '%08d' % label}

        return data, label


def processing_testing_configurations(data, label):  # pylint: disable=invalid-name,too-many-locals
    with open('testing_configurations.json') as configfile:
        cfile = json.load(configfile)

        for regexp in ('url', 'cgi'):
            for key, value in cfile['configurations'].iteritems():
                label += 100
                if key == 'default':
                    pass
                else:
                    if value.get('backend'):
                        backends = [
                            {
                                'name': '{}'.format(group),
                                'version': 'trunk'
                            } for group in cfile['synonyms']['backend'][value['backend']]
                        ]
                    else:
                        backends = [
                            {
                                'name': '{}'.format(group),
                                'version': 'trunk'
                            } for group in cfile['synonyms']['backend']['default']
                        ]

                    modules = [
                        {'stats_eater': {}},
                        {
                            'balancer2': OrderedDict([
                                ('weighted2', {}),
                                ('unique_policy', {}),
                                ('generated_proxy_backends', OrderedDict([
                                    ('proxy_options', {
                                        'connect_timeout': '100ms',
                                        'backend_timeout': '10s',
                                        'keepalive_count': 10
                                    }),
                                    ('gencfg_groups', backends)
                                ]))
                            ])
                        }
                    ]

                    cgi_rewrite = {}
                    if value.get('cgi'):
                        cgis = ''.join(
                            ['&source={}'.format(c) for c in value['cgi']['source']]
                        )

                        cgi_rewrite = {
                            'regexp': '(.*)',
                            'rewrite': '%1{}'.format(cgis)
                        }

                    url_rewrite = {}
                    if regexp == 'url':
                        matcher = {'url': '/search/{}((/|\\\\?).+)?'.format(key)}
                        url_rewrite = {
                            'regexp': '/search/{}(/)?(.*)?'.format(key),
                            'rewrite': '/%2', 'split': 'url'
                        }
                    else:
                        if 'collection' in value['urlfilter']:
                            matcher = {'url': '/{}?'.format(value['urlfilter']['collection'])}
                        else:
                            matcher = {
                                'cgi': '{}={}'.format(
                                    value['urlfilter'].keys()[0],
                                    value['urlfilter'].values()[0]
                                ),
                                'surround': True
                            }

                    if cgi_rewrite or url_rewrite:
                        actions = list()
                        if cgi_rewrite:
                            actions.append(cgi_rewrite)
                        if url_rewrite:
                            actions.append(url_rewrite)

                        modules.insert(0, {'rewrite': {'actions': actions}})

                    result = {
                        'regexp_section': {
                            'matcher': {
                                'match_fsm': matcher
                            },
                            'modules': modules
                        }
                    }

                    write_yaml(key, regexp, result)

                    if data.get(PATTERN.format(key, regexp)) is None:
                        data[PATTERN.format(key, regexp)] = {'order': '%08d' % label}

    return data, label


def main():  # pylint: disable=too-many-locals,too-many-statements
    label = 100
    with open('LABELS.json') as label_file:
        try:
            data = json.load(label_file)
        except ValueError:
            data = {}

    with open('LABELS.json', 'w') as label_file:
        data, label = processing_testing_configurations(data, label)
        data, label = processing_balancer_tags(data, label)
        default_upstream()

        label_file.write(json.dumps(data, indent=2))


if __name__ == '__main__':
    main()
