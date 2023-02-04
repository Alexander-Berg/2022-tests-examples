# -*- coding: utf-8 -*-

if __name__ == "__main__":
    act = [{'pre': 1, 'post': 10, 'sup': 2},
         {'pre': 2, 'post': 20, 'sup': 2},
         {'pre': 3, 'post': 30, 'sup': 2},
         {'pre': 4, 'post': 40, 'sup': 2}
         ]
    exp = [{'pre': 1, 'post': 11},
         {'pre': 2, 'post': 20},
         {'pre': 3, 'post': 30}
         ]

    # pop extra keys from actual
    for line in act:
        for key in line.keys():
            if key not in exp[0]:
                line.pop(key)

    # Convert every dict to tuple of tuples
    act_modified = [tuple((key, line[key]) for key in sorted(line.keys())) for line in act]
    exp_modified = [tuple((key, line[key]) for key in sorted(line.keys())) for line in exp]

    from collections import Counter

    act_cnt = Counter(act_modified)
    exp_cnt = Counter(exp_modified)
    act_cnt.subtract(exp_cnt)

    for item in act_cnt.values():
        if item<0 or (item>0 and not 1):
            pass
            # return False
    # return True
            # return False
    # return True

    pass



