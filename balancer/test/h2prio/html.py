# -*- coding: utf-8 -*-
import json

__TEMPLATE = '''<!DOCTYPE HTML>
<html>
    <head>
        <script src="https://code.highcharts.com/highcharts.js"></script>
        <script src="https://code.highcharts.com/modules/xrange.js"></script>
        <script src="https://code.highcharts.com/modules/exporting.js"></script>
        <script type="text/javascript">
            function addChart(elem_id, info) {
                new Highcharts.Chart({
                    chart: {
                        type: 'xrange',
                        zoomType: 'x',
                        renderTo: document.getElementById(elem_id)
                    },
                    title: {
                        text: info['name']
                    },
                    xAxis: {
                        type: 'linear'
                    },
                    yAxis: {
                        title: {
                            text: 'streams'
                        },
                        categories: info['streams'],
                        reversed: true
                    },
                    series: [{
                        name: 'Data',
                        // pointPadding: 0,
                        // groupPadding: 0,
                        borderColor: 'gray',
                        pointWidth: 20,
                        data: info['data'],
                        dataLabels: {
                            enabled: true,
                            formatter: function() { return this.x2 - this.x; }
                        },
                        turboThreshold: info['threshold']
                    }],
                });
            }
        </script>
        <script type="text/javascript">
            window.onload = function () {
                let all_info = %s;
                let cnt = 0;
                for (info of all_info) {
                    let elem_id = 'container-' + cnt;
                    cnt += 1;

                    let elem = document.createElement('div');
                    elem.id = elem_id;
                    elem.style.cssText = "min-width: 310px; height: 300px; margin: 0 auto";
                    document.body.append(elem);
                    addChart(elem_id, info);
                }
            };
        </script>
    </head>
    <body>
    <div>
    %s
    </div>
    </body>
</html>
'''


def gen_html(graph, info):
    if isinstance(info, dict):
        info = [info]
    return __TEMPLATE % (json.dumps(info), graph)


def dump_html(graph, info, path):
    res = gen_html(graph, info)
    with open(path, 'w') as f:
        f.write(res)
