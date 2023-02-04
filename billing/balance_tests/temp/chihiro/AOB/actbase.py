# -- coding: utf-8 --


def get_data(i):
        base = open('/Users/chihiro/PycharmProjects/python-tests/temp/chihiro/AOB/data.txt', 'r')
        line =[int(e.strip()) for e in base][i]
        base.close()
        return line

def input(date):
    rates = str(date)+'\n'
    with open('/Users/chihiro/PycharmProjects/python-tests/temp/chihiro/AOB/data.txt', 'a') as rates_file:
        rates_file.write(rates)
    rates_file.close()
