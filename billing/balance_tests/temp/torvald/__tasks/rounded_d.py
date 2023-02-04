from decimal import getcontext
from decimal import Decimal as D, ROUND_FLOOR
from decimal import ROUND_HALF_UP

getcontext().prec = 15

def rounded_delta_pre(n, quant):
    delta = 0
    total = 0
    rounded_total = 0

    # actual = D('30') / D('31')
    # getcontext().prec = 3 if actual > 1 else 2
    # rounded = D('30') / D('31')
    # getcontext().prec = 15
    #
    actual = D('30') / D('29')
    rounded = (D('30') / D('29')).quantize(D('0.00'))

    # for item in range(n):
    #     current_rounded = rounded
    #     total += actual
    #     rounded_total += rounded
    #     delta += actual - rounded
    #     if abs(delta) >= quant:
    #         rounded_total += quant * (delta / abs(delta))
    #         current_rounded += quant * (delta / abs(delta))
    #         delta -= quant * (delta / abs(delta))
    #     yield actual, total, current_rounded, rounded_total, delta
    rounded_n = rounded
    delta_1 = actual - rounded
    actual_n = actual * n
    delta_n = actual_n - actual_n.quantize(D('0.00'), ROUND_FLOOR)
    if delta_n < delta_1:
        return rounded + quant
    else:
        return rounded


def rounded_delta(n, ratio, quant):
    delta = 0

    actual = ratio
    rounded = ratio.quantize(D('0.00'))


    for item in range(n):
        current_rounded = rounded
        delta += actual - rounded
        if abs(delta) >= quant:
            current_rounded += quant * (delta / abs(delta))
            delta -= quant * (delta / abs(delta))
        yield current_rounded


def rounded_delta_cool(n, ratio, quant):

    actual = ratio
    rounded = ratio.quantize(D('0.00'))

    delta_1 = actual - rounded
    actual_n = actual * n
    delta_n = actual_n - actual_n.quantize(D('0.00'), ROUND_HALF_UP)
    if delta_n:
        if abs(delta_n) <= abs(delta_1):
            return rounded + (quant * (delta_1 / abs(delta_1)))
        else:
            return rounded
    else:
        return rounded


def rounded_delta_billing(total_parts, prev_part_num, cur_part_num, total_value, quantum=D( '0.01' )):
    if total_parts == 0 and total_value == 0 or prev_part_num == cur_part_num:
        return 0
    return (D(total_value) * cur_part_num / total_parts).quantize(quantum, ROUND_HALF_UP) - \
    (D(total_value) * prev_part_num / total_parts).quantize(quantum, ROUND_HALF_UP)


# # rounded_delta_cool(3, D('30') / D('31'), D('0.01'))
# n = 31
# actual_sum = 0
# actual = D('30') / D(str(n))
# rounded_sum = 0
# for day in range(n):
#     actual_sum += D('30') / D(str(n))
#     current = rounded_delta_cool(day+1, D('30') / D(str(n)), D('0.01'))
#     rounded_sum += current
#     delta = actual_sum - rounded_sum
#     print '{:4}: \t {} \t {} \t {} \t {} \t {}'.format(day+1, actual, current, actual_sum,rounded_sum, delta)
#
# # a = rounded_delta_pre(6, D('0.01'))
# rounded_delta_cool(31, D('30') / D('31'), D('0.01'))

n = 31
for day in range(n):
    print '{} {}'.format(day+1, rounded_delta_billing(31, day, day+1, 30))
# pass