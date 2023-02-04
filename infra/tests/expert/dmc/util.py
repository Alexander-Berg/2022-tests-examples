from walle.expert.types import WalleAction, CheckType, CheckStatus, HwWatcherCheckStatus


def possible_action_and_checks_combinations(actions=None):
    if actions is None:
        actions = WalleAction.ALL_DMC.copy()

    def _checks_generator():
        checks_list = CheckType.ALL_AVAILABILITY.copy()

        for checks in checks_list:
            if not isinstance(checks, (list, tuple)):
                checks = [checks]

            yield checks

    for action in actions:
        if action in [WalleAction.HEALTHY, WalleAction.WAIT, WalleAction.DEACTIVATE, WalleAction.FIX_DNS]:
            yield action, None
        else:
            for checks in _checks_generator():
                yield action, checks


def break_check(reasons, check_type, status=CheckStatus.MISSING):
    reasons[check_type] = {"status": status}


def fail_check(reasons, check_type):
    check = reasons[check_type]

    check["status"] = CheckStatus.FAILED
    if check_type == CheckType.MEMORY:
        check["metadata"]["results"]["ecc"] = {
            "slot": "DIMM-1",
            "status": HwWatcherCheckStatus.UNKNOWN,
            "reason": "reason-mock",
            "comment": "ecc errors uncorrectable were",
        }
