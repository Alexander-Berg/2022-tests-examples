from balance import balance_db as db

all_acts = db.balance().execute(
    "SELECT e.object_id, e.state, e.error, e.TRACEBACK FROM t_act_internal ai inner join t_export e on e.object_id = ai.id WHERE ai.dt = TO_DATE('2016-10-31 00:00:00', 'YYYY-MM-DD HH24:MI:SS') and e.state = 2 and e.CLASSNAME = 'Act'")
print all_acts
