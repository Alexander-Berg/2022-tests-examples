from billing.yandex_pay_admin.yandex_pay_admin.api.handlers.internal.base import BaseInternalHandler

DISABLED_TVM_HANDLERS = set()


def test_tvm_checking(api_handlers):
    for cls in api_handlers:
        if not issubclass(cls, BaseInternalHandler):
            continue

        if cls not in DISABLED_TVM_HANDLERS:
            assert cls.CHECK_TVM, f'Tvm checking is disabled for {cls.__name__}'
        else:
            assert not cls.CHECK_TVM, (
                f'Tvm checking is enabled for {cls.__name__}. '
                'But this handler added to the excluding set, '
                'please remove it from the set if tvm checking needed'
            )
