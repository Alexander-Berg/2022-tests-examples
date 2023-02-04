from balance import balance_steps as steps

f = open('/Users/aikawa/Documents/acts_export_febr', 'r')
acts_ids = f.read().split()

for act_id in acts_ids:
    print steps.ExportSteps.export_oebs(act_id=act_id)
