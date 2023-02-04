# import re
#
# text = "'u'Functionality!\n  * !!(\u0441\u0435\u0440)#CREDIT-1.1!! check1\n  * check2\n  * check3"
#
# m = re.search(r'(!!\(\\u0441\\u0435\\u0440\)\#[A-Z]+-[0-9].*[0-9])', text)
#
# print m.start(), m.group(0)
#
# # import re
# # p = re.compile("[a-z]")
# # for m in p.finditer('a1b2c3d4'):
# #     print m.start(), m.group()