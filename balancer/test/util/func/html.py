HTML = """<!DOCTYPE HTML>
<html>
    <body>
        <script type="text/javascript">%s</script>
        <script type="text/javascript">%s</script>
        <script type="text/javascript">%s</script>
        <script type="text/javascript">
            window.onload = function () {
                new Highcharts.Chart({
                    chart: {
                        type: 'line',
                        renderTo: document.getElementById('container')
                    },
                    title: {
                        text: 'RPS'
                    },
                    xAxis: {
                        plotBands: %s
                    },
                    yAxis: {
                        min: 0,
                        title: {
                            text: 'rps'
                        },
                    },
                    tooltip: {
                        shared: true,
                    },
                    plotOptions: {
                        area: {
                            stacking: 'normal',
                            lineColor: '#666666',
                            lineWidth: 1,
                            marker: {
                                lineWidth: 1,
                                lineColor: '#666666'
                            }
                        },
                        line: {
                            marker: {
                                enabled: false
                            }
                        }
                    },
                    series: %s
                });
            };
        </script>
        <div id="container" style="min-width: 310px; height: 300px; margin: 0 auto"></div>
    </body>
</html>
"""
