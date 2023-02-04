# coding=utf-8
import os

import balance.balance_db as db
from actbase import get_data
from data import Prepare


def check():
    for i in range(6):
        act_id=get_data(i)
        id = db.balance().execute('select external_id from bo.T_ACT where id= :act_id', {'act_id': act_id})[0][
            'external_id']
        state = db.balance().execute('select state from cmp.AOB_TR_CMP_DATA where eid= :eid', {'eid': id})
        if i==0:
            if (state==[]):
                print('test_',i,': pass ')
            else:
                print('test_',i,': failed ')
        if i>0:
            if state==[]:
                print('test_',i,': failed ')
            else:
                if state[0]['state']==i:
                    print('test_',i,': pass ')
                else:
                    print('test_',i,': failed ')

def search_data():
    path='/Users/chihiro/PycharmProjects/python-tests/temp/chihiro/AOB/data.txt'
    if os.path.getsize(path) == 0:
        Prepare()

def search_data2():
    path='/Users/chihiro/PycharmProjects/python-tests/temp/chihiro/AOB/data.txt'
    for i in range(6):
        act_id=get_data(i)
        id = db.balance().execute('select external_id from bo.T_ACT where id= :act_id', {'act_id': act_id})
        if id==[]:
            os.remove(path)
            break


if __name__ == "__main__":
    search_data2()