import balance.balance_api as api
from balance import balance_steps as steps
print api.test_balance().GetNotification(1, 363040206)


steps.CommonSteps.get_pickled_value('''sel''')
# {"7": {"1": {"thresholds": {"null": 4000, "RUB": 120000}, "start_dt": null, "end_dt": null, "turnover_firms": [1]},
#        "27": {"thresholds": {"BYN": 650}, "start_dt": null, "end_dt": null, "turnover_firms": [27, 1]}},
#  "11": {"1": {"thresholds": {"null": 4000}, "start_dt": null, "end_dt": null, "turnover_firms": [1]}, "111": {"thresholds": {"null": 4000}, "start_dt": null, "end_dt": null, "turnover_firms": [111]}}}