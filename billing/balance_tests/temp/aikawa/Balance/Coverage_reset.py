from balance import balance_api as api
from balance import balance_steps as steps

# print api.test_balance().BalanceCommonVersion()

# steps.ClientSteps.create()

a = api.coverage().server.Coverage.Reset()
steps.ClientSteps.create()
a = api.coverage().server.Coverage.Collect('aikawa-coverage', False)
for key in a['aikawa-coverage']:
    print key, a['aikawa-coverage'][key]['notrun_lines']
# # b.sort()
# pprint.pprint(b)
