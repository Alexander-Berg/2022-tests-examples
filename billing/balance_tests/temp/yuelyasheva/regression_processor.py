import csv
import os
import statistics

list_of_files = os.listdir('/Users/yuelyasheva/Downloads/tests_stat')
tests = {}
for file in list_of_files:
    with open('/Users/yuelyasheva/Downloads/tests_stat/' + file) as csvfile:
        test_stats = csv.DictReader(csvfile)
        for row in test_stats:
            if row['Test Name'] in tests:
                tests[row['Test Name']]['Duration'].append(int(row['Duration(ms)']))
            else:
                tests[row['Test Name']] = {'Duration': [int(row['Duration(ms)'])],
                                           'OK': 0,
                                           'Failure': 0,
                                           'Ignored': 0,
                                           'Muted failure': 0}
            tests[row['Test Name']][row['Status']] += 1

result = {}
for test in tests:
    result[test] = {'Min duration': min(tests[test]['Duration']),
                    'Max duration': max(tests[test]['Duration']),
                    'Mean duration': statistics.mean(tests[test]['Duration']),
                    'Median duration': statistics.median(tests[test]['Duration']),
                    'Success': tests[test]['OK'],
                    'Failure': tests[test]['Failure'],
                    'Muted failure': tests[test]['Muted failure'],
                    'Ignored': tests[test]['Ignored']}

result_filename = 'Regressions stats.csv'
with open('/Users/yuelyasheva/' + result_filename, 'w') as csvfile:
    fieldnames = ['Test name', 'Min duration, s', 'Max duration, s', 'Mean duration, s', 'Median duration, s', 'Success',
                  'Failure', 'Muted failure', 'Ignored']
    writer = csv.DictWriter(csvfile, fieldnames=fieldnames)

    writer.writeheader()
    for test in result:
        writer.writerow({'Test name': test,
                         'Min duration, s': result[test]['Min duration']/1000,
                         'Max duration, s': result[test]['Max duration']/1000,
                         'Mean duration, s': result[test]['Mean duration']/1000,
                         'Median duration, s': result[test]['Median duration']/1000,
                         'Success': result[test]['Success'],
                         'Failure': result[test]['Failure'],
                         'Muted failure': result[test]['Muted failure'],
                         'Ignored': result[test]['Ignored']})