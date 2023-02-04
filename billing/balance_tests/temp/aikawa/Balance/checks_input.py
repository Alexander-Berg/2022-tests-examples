from balance import balance_db as db
from balance import balance_steps as steps

query = '''SELECT input FROM T_EXPORT WHERE OBJECT_ID = 333383883'''
# print steps.CommonSteps.get_pickled_value(query, key='input')
print steps.ExportSteps.get_export_input(333383883, 'Payment', 'CASH_REGISTER')