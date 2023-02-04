text = open('/Users/aikawa/Work/SQL/t_order.bad')
target = open('/Users/aikawa/Work/SQL/t_order_corr.bad', 'wb')

text_list = []
for row in text:
    row = row.split('||+|')
    text_list.append(row)

for key, item in enumerate(text_list):
    if len(item) == 8:
        text_list[key] = text_list[key]+text_list[key+1]
        text_list.pop(key+1)


for row in text_list:
    if len(row)  == 74:
        row.pop(8)

# for row in text_list:
#     for key, elem in enumerate(row):
#         row[key] = row[key].rstrip()
#     print row
#     target.write('||+|'.join(row)+'\n')

