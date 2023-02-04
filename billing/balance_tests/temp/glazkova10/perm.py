from  balance.balance_steps.other_steps.py import PermissionsSteps
import balance.balance_db as db

def unique_role (role_id, addperm, deleteperm):
    role_id = 0
    addperm = input() #вводим права, которые должны быть в роли через пробел
    deleteperm = input() #вводим права, которых не должно быть в роли через пробел


    sum_addperm = len(addperm.split(' ')) #вычисляем кол-во прав, которые надо добавить
    sum_deleteperm = len(deleteperm.split(' ')) #вычисляем кол-во прав, которые надо оторвать

    if addperm==''and deleteperm=='':
      return role_id
    elif addperm!='' and deleteperm=='':
        find_role=db.balance().execute("select role_id from (select count(perm) sumperm ,  role_id from t_role where perm in (:addperm) group by role_id) where sumperm=:sum_addperm and rownum = 1, {'addperm':addperm, 'sum_addperm':sum_addperm}")
        if find_role!='':
            return find_role
        else: # создаем копию роли и добавляем нужные права
    elif addperm=='' and deleteperm!='': #
        find_role = db.balance().execute("select role_id from (select count(perm) sumperm ,  role_id from t_role where perm not in (:deleteperm) group by role_id) where sumperm=2 and rownum = 1, {'deleteperm':deleteperm, 'sum_deleteperm':sum_deleteperm}")
        if find_role!='':
            return find_role
        else:  # создаем копию роли и отрываем нужные права
    elif addperm == '' and deleteperm != '':
        find_role = db.balance().execute("select adding.role_id from (select role_id, sumperm  from (select count(perm) sumperm ,  role_id from t_role where perm in (:addperm) group by role_id) where sumperm=:sum_addperm) adding left join (select role_id, sumperm from (select count(perm) sumperm , role_id from t_role where perm in (:deleteperm) group by role_id) where sumperm=:sum_deleteperm) deletion ON adding.role_id = deletion.role_id where deletion.role_id is null and rownum = 1, {'addperm':addperm, 'sum_addperm':sum_addperm, 'deleteperm':deleteperm, 'sum_deleteperm':sum_deleteperm}")
        if find_role != '':
            return find_role
        else:  # создаем копию роли, настраиваем необходимые права






