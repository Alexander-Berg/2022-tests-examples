#-*- coding: utf-8 -*-

## переименование файла
import os

# path = "C:\Users\sandyk\Desktop\Python"
path = "C:\Users\sandyk\Desktop\mt"
dirs = os.listdir( path )

#
# for file in dirs:
#    if fnmatch.fnmatch(file, 'billing*'):
#        st = file[0:-4] + '(VISIBLE).txt'
#        print st
#        os.rename (file, st)

for file in dirs:
    print '--'
    print file
    if file.startswith('billing'):
        r = str(file[:-11]) + 'HIDDEN.txt'
        print r
        os.rename(os.path.join(path, file), os.path.join(path, r))
        # os.rename(file, r)
        # rename(file, file.replace('VISIBLE', '', 1))


## замена в файле
# path = "C:\Users\sandyk\Desktop\Python"
# dirs = os.listdir( path )
#
# for file in dirs:
#     if fnmatch.fnmatch(file, 'billing*'):
#         print file
#         r = fileinput.input(file, inplace=True)
#         print r
#         for line in r:
#             line = line.replace('contract-edit', 'contract')
#             sys.stdout.write(line)
