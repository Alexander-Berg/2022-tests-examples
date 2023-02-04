OWNER(g:awacs)

PY3TEST()

DEPENDS(
    infra/awacs/vendor/awacs/tests/deps
    jdk
)

DATA(
    arcadia/infra/awacs/vendor/awacs/tests/deps
)

TEST_SRCS(
    __init__.py
    test_aab_cookie_verify.py
    test_accesslog.py
    test_active_check_reply.py
    test_antirobot.py
    test_antirobot_macro.py
    test_antirobot_wrapper.py
    test_balancer2.py
    test_cgi_hasher.py
    test_chain.py
    test_click_macro.py
    test_cookies.py
    test_error_document.py
    test_errorlog.py
    test_expgetter.py
    test_extended_http_macro.py
    test_flags_getter.py
    test_func.py
    test_geobase_macro.py
    test_hdrcgi.py
    test_headers.py
    test_http.py
    test_http2.py
    test_icookie.py
    test_include_upstreams.py
    test_instance_macro.py
    test_ipdispatch.py
    test_log_headers.py
    test_main.py
    test_pinger.py
    test_prefix_path_router.py
    test_proxy.py
    test_regexp.py
    test_regexp_host.py
    test_regexp_path.py
    test_remote_log.py
    test_report.py
    test_request_replier.py
    test_response_headers_if.py
    test_rewrite.py
    test_rpc_rewrite.py
    test_rpc_rewrite_macro.py
    test_rps_limiter.py
    test_shared.py
    test_slb_ping_macro.py
    test_srcrwr.py
    test_srcrwr_ext.py
    test_ssl_sni.py
    test_threshold.py
    test_validators.py

    test_l7_macro/test_announce_check_reply.py
    test_l7_macro/test_antirobot.py
    test_l7_macro/test_compat.py
    test_l7_macro/test_domains.py
    test_l7_macro/test_icookie.py
    test_l7_macro/test_rps_limiter.py
    test_l7_macro/test_tls_settings.py
    test_l7_macro/test_trust.py
    test_l7_macro/test_uaas.py
    test_l7_macro/test_validation_and_expand.py
    test_l7_macro/test_webauth.py
    test_l7_macro/test_headers.py
    test_l7_macro/test_rewrite.py

    test_l7_upstream_macro/test_balancer_settings.py
    test_l7_upstream_macro/test_balancing_schemes.py
    test_l7_upstream_macro/test_compression.py
    test_l7_upstream_macro/test_headers.py
    test_l7_upstream_macro/test_matcher.py
    test_l7_upstream_macro/test_monitoring.py
    test_l7_upstream_macro/test_rps_limiter.py
    test_l7_upstream_macro/test_uaas.py
    test_l7_upstream_macro/test_validation.py
)

PEERDIR(
    contrib/python/mock
    contrib/python/flaky
    contrib/python/pytest-vcr
    contrib/python/vcrpy
    infra/swatlib
    infra/awacs/vendor/awacs
    infra/awacs/vendor/awacs/tests/awtest
)

TIMEOUT(600)
SIZE(MEDIUM)

NO_CHECK_IMPORTS()

END()
