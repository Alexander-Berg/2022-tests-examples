from textwrap import dedent

import maps.infra.balancer_deployer.sign as sign


def test_sign():
    commit_id = 2
    content = '''
        ---
        key:
          - value
        other_key: value'''
    yaml_sign = sign.sign(content=content)
    signed_content = sign.yaml_with_header(yaml=content, sign=yaml_sign,
                                           commit_id=commit_id)

    assert content != signed_content
    assert sign.yaml_content(signed_content) == content
    assert (content, yaml_sign, commit_id) == sign.decompose_yaml(signed_content)


def test_decompose_new_yaml():
    signed_content_new = dedent('''\
        ### balancer_deployer_sign: 95d9334ba0e987d7a3d20f78187e0576
        ### balancer_deployer_commit_id: 9192875
        l7_macro:
          version: 0.3.6
          core:
            trust_x_forwarded_for_y: true
          health_check_reply:
            compat:
              replaced_upstream_id: awacs-balancer-health-check
          http: {}
          https:
            enable_http2: true
            enable_tlsv1_3: true
            certs:
              - id: core-renderer-tiles.maps.yandex.net_v2
    ''')

    content, yaml_sign, commit_id = sign.decompose_yaml(signed_content_new)
    assert content == dedent('''\
        l7_macro:
          version: 0.3.6
          core:
            trust_x_forwarded_for_y: true
          health_check_reply:
            compat:
              replaced_upstream_id: awacs-balancer-health-check
          http: {}
          https:
            enable_http2: true
            enable_tlsv1_3: true
            certs:
              - id: core-renderer-tiles.maps.yandex.net_v2
    ''')
    assert yaml_sign == '95d9334ba0e987d7a3d20f78187e0576'
    assert commit_id == 9192875


def test_decompose_old_yaml():
    signed_content_old = dedent('''\
        ### balancer_deployer_sign: 95d9334ba0e987d7a3d20f78187e0576
        ### balancer_deployer_commit_order: 9192875
        ### balancer_deployer_commit_id: 9192875
        l7_macro:
          version: 0.3.6
          core:
            trust_x_forwarded_for_y: true
          health_check_reply:
            compat:
              replaced_upstream_id: awacs-balancer-health-check
          http: {}
          https:
            enable_http2: true
            enable_tlsv1_3: true
            certs:
              - id: core-renderer-tiles.maps.yandex.net_v2
    ''')

    content, yaml_sign, commit_id = sign.decompose_yaml(signed_content_old)
    assert content == dedent('''\
        l7_macro:
          version: 0.3.6
          core:
            trust_x_forwarded_for_y: true
          health_check_reply:
            compat:
              replaced_upstream_id: awacs-balancer-health-check
          http: {}
          https:
            enable_http2: true
            enable_tlsv1_3: true
            certs:
              - id: core-renderer-tiles.maps.yandex.net_v2
    ''')
    assert yaml_sign == '95d9334ba0e987d7a3d20f78187e0576'
    assert commit_id == 9192875
