import pickle

import btestlib.reporter as reporter

datafile = '../../test_list.dat'
# datafile = 'test_list.dat'

def load_data():
    with open(datafile) as f:
        p = f.read()
        test_list = pickle.loads(p)
    return test_list

def patch_data(test_list):
    for item in test_list:
        str = item['item'].replace('::', '/')
        _, item['dir'], item['file'], item['test'] = str.split('/')
    return test_list

def restruct_data(test_list):
    struct = {}
    for item in test_list:
        dir = item['dir']
        if dir not in struct:
            struct[dir] = {}
        # priority = item['priority'] or 'None'
        # if priority not in struct[item['dir']]:
        #     struct[item['dir']][priority] = []
        # struct[item['dir']][priority].append(item)
        file = item['file']
        if file not in struct[dir]:
            struct[dir][file] = []
        struct[dir][file].append(item)
    return struct

def vis(test_list):

    def priority_mark (priority):
        if priority == 'low':
            return '[L]'
        elif priority == 'mid':
            return '[M]'
        elif priority == 'high':
            return '[H]'
        elif priority is None:
            return '[M]'

    def speed_mark(speed):
        return '[ ]' if speed else '[>]'

    indent = '  '
    for item in test_list:
        reporter.log('===={0}===='.format(item))
        # for priority in test_list[item]:
        #     reporter.log('\t'+priority)
        #     for test in test_list[item][priority]:
        #         reporter.log('\t\t{0}/{1}'.format(test['file'], test['test']))
        for file in test_list[item]:
            reporter.log('{0}**{1}**'.format(indent, file))
            for test in test_list[item][file]:
                xfail = skipif = 0
                if test['xfail'] is not None:
                    xfail = 1
                    if 'reason' in test['xfail'].kwargs:
                        xfail = test['xfail'].kwargs['reason']
                if test['skipif'] is not None:
                    skipif = 1
                    if 'reason' in test['skipif'].kwargs:
                        skipif = test['skipif'].kwargs['reason']
                # reporter.log('{0}{1} {2}'.format(indent*2,pr_mark(test['priority']),test['test']))
                # reporter.log('{0}P: {1:<4}, C: {2:<20}, T: {3:<13}, S: {4:<6}, X: {5:<10}, Skip: {6:<10}'.format(indent*3)
                #                                    , test['priority'] or '--None--'
                #                                    , '[{}]'.format(test['categories'] or '--None--')
                #                                    , test['ticket'] or '--None--'
                #                                    , test['speed'] or '--None--'
                #                                    , xfail or '--None--'
                #                                    , skipif or '--None--')

                categories_indent = 80 if len(test['test']) >= 60 else 60
                special_indent = 40 if (test['categories'] and len(test['categories'])) >= 30 else 30
                reporter.log('{0}{1}{2}{3} {4:<{5}} | {6:<{7}} | {8}{9}'.format(indent * 2)
                             , priority_mark(test['priority'])
                             , speed_mark(test['slow'])
                             , '[{0}]'.format(test['tickets'] or 'BALANCE-_____')
                             , test['test']
                             , categories_indent
                             , '[{0}]'.format(test['categories'] or 'empty')
                             , special_indent
                             , 'xFail: {0}'.format(xfail) if xfail else ''
                             , 'skipIf: {0}'.format(skipif) if skipif else ''
                             )

def universal (test_list, keys):
    #key: ('dir', 'file', 'test', 'priority', 'categories', 'ticket', 'speed', 'xfail', 'skipif')
    groupped = dict()
    current = dict()
    for item in test_list:
        current = current.copy()
        groupped
        for key in keys:
            if key in item:
                if key not in current:
                    current[item[key]] = dict()
                current = current[item[key]]


if __name__ == "__main__":
    test_list = load_data()
    test_list = patch_data(test_list)
    #For analysis:
    # universal(test_list, ['dir', 'file', 'test'])
    #For wiki:
    restruct = restruct_data(test_list)
    vis(restruct)
    pass

    # reporter.log((str(test_list)))
