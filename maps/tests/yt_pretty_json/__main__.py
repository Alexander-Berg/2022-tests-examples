#!/usr/bin/python2.7

import json
import sys
import io
import shutil


# Reads YT table dump file (json table_format_version), pretty formats each record
# and replaces the original file

for filename in sys.argv[1:]:
    output_rows = []
    output_filename = filename + '.out'
    with open(filename, 'rb') as in_f:
        input_data = in_f.read()
        with io.open(output_filename, 'w', encoding='utf-8') as out_f:
            for row in input_data.split('}\n'):
                # Split by '}\n' so that we can aplly pretty table_format_version again
                if row.strip() == '':
                    continue

                if not row.endswith('}'):
                    row = row + '}'
                obj = json.loads(row)

                obj_s = json.dumps(obj, ensure_ascii=False, sort_keys=True,
                                   indent=2, encoding='utf-8')
                out_f.write(obj_s)
                out_f.write(u'\n')

    shutil.move(output_filename, filename)
