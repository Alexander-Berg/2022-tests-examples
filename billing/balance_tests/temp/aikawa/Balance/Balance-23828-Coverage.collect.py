import pprint

from balance import balance_api as api
from balance import balance_steps as steps

# a = api.coverage().server.Coverage.Reset()
print api.coverage().server.Coverage.Collect(None, True)

a = api.coverage().server.Coverage.Collect(None, True)
steps.ClientSteps.create()
a = api.coverage().server.Coverage.Collect(None, True)
pprint.pprint(a.keys())
b = a['aikawa-coverage'].keys()
pprint.pprint(a['aikawa-coverage'].keys())
pprint.pprint(a[''].keys())
