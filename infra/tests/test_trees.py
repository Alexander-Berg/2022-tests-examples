from infra.reconf_juggler.resolvers import AbstractResolver, RootResolver
from infra.reconf_juggler.trees import Node, NodeSet
from infra.reconf_juggler.trees.locations import Locations


def test_node_without_branches():
    structure = {
        'children': {
            'subnode': {
                'children': {
                    'another': {
                        'children': {'endpoint': None},
                    },
                    'endpoint': None,
                },
            },
            'endpoint': None,
        },
    }
    expected = {
        'children': {
            'subnode': {  # unchanged - parent node has no name to for prefix
                'children': {
                    'subnode_another': {  # name changed
                        'children': {'endpoint': None},
                    },
                    'endpoint': None,
                },
            },
            'endpoint': None,
        },
    }

    assert expected == Node(structure, resolver=None)


def test_nodeset_without_branches():
    structure = {
        'root': {
            'children': {
                'subnode': {
                    'children': {'endpoint': None},
                },
                'endpoint': None,
            },
        },
    }
    expected = {
        'root': {
            'children': {
                'root_subnode': {  # subnodes has parent node name as a prefix
                    'children': {'endpoint': None},
                },
                'endpoint': None,
            },
        },
    }

    assert expected == NodeSet(structure, resolver=None)


def test_endpoints_resolver():
    structure = {
        'root': {
            'children': {
                'subnode': {
                    'children': {'endpoint_0': None},
                },
                'endpoint_1': None,
            },
        },
    }
    expected = {
        'root': {
            'children': {  # subnode removed (zero resolved endpoints)
                'endpoint_1': None,
            },
        },
    }

    class OverridedResolver(AbstractResolver):
        def resolve_query(self, query):
            if query == 'endpoint_1':
                return 2

            return 0

    resolver = RootResolver()
    resolver['juggler']['instances_count'] = OverridedResolver()

    assert expected == NodeSet(structure, resolver=resolver)


def test_locations():
    structure = {
        'root': {
            'children': {
                'subnode': {
                    'children': {'endpoint_0': None},
                    'tags': ['grp_subnode'],
                },
                'endpoint_1': None,
            },
        },
    }
    expected = {
        'root': {
            'children': {
                'root_man': {
                    'children': {
                        'endpoint_1&datacenter=man': None,
                        'root_man_subnode': {
                            'children': {
                                'endpoint_0&datacenter=man': None,
                            },
                            'tags': ['geo_man', 'grp_subnode'],
                        },
                    },
                    'tags': ['geo_man'],
                },
                'root_msk': {
                    'children': {
                        'root_msk_iva': {
                            'children': {
                                'endpoint_1&datacenter=iva': None,
                                'root_msk_iva_subnode': {
                                    'children': {
                                        'endpoint_0&datacenter=iva': None,
                                    },
                                    'tags': ['dc_iva', 'geo_msk', 'grp_subnode'],
                                },
                            },
                            'tags': ['dc_iva', 'geo_msk'],
                        },
                        'root_msk_myt': {
                            'children': {
                                'endpoint_1&datacenter=myt': None,
                                'root_msk_myt_subnode': {
                                    'children': {
                                        'endpoint_0&datacenter=myt': None,
                                    },
                                    'tags': ['dc_myt', 'geo_msk', 'grp_subnode'],
                                },
                            },
                            'tags': ['dc_myt', 'geo_msk'],
                        },
                    },
                    'tags': ['geo_msk'],
                },
                'root_sas': {
                    'children': {
                        'endpoint_1&datacenter=sas': None,
                        'root_sas_subnode': {
                            'children': {
                                'endpoint_0&datacenter=sas': None,
                            },
                            'tags': ['geo_sas', 'grp_subnode'],
                        },
                    },
                    'tags': ['geo_sas'],
                },
                'root_vla': {
                    'children': {
                        'endpoint_1&datacenter=vla': None,
                        'root_vla_subnode': {
                            'children': {
                                'endpoint_0&datacenter=vla': None,
                            },
                            'tags': ['geo_vla', 'grp_subnode'],
                        },
                    },
                    'tags': ['geo_vla'],
                },
            },
        },
    }

    assert expected == Locations(structure, resolver=None)
