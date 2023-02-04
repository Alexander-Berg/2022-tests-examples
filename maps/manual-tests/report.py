#!/usr/bin/python
# -*- coding: utf-8 -*-

from textwrap import dedent

class ModuleRequiredError(Exception):
    def __init__(self, name):
        Exception.__init__(self,
                "\n\n{name} module not found"
                "\nrun 'pip install {name}'"
                "\nor 'easy_install {name}\n".format(name=name))


try:
    import pystache
except ImportError:
    raise ModuleRequiredError('pystache')

import os
import subprocess
import itertools

TEMPLATE = (
r'''<script src="http://yandex.st/jquery/1.7.1/jquery.min.js" type="text/javascript"></script>
<script type="text/javascript">
$(document).ready(function() {
    $("img.rollover").hover(
        function() { this.src = this.src.replace("screenshot_new.png", "screenshot.png"); },
        function() { this.src = this.src.replace("screenshot.png", "screenshot_new.png"); }
    );

    $(".overwrite").submit(
        function(event) {
            event.preventDefault();
            if (!confirm("Overwrite reference screenshot?"))
                return;
            var sshots_dir = $(this).attr("action");
            $.post("overwrite",
                   { dir: sshots_dir },
                   function(data) {
                        if (data.status != "succeeded") {
                            alert("FAILED\n Error: " + data.error)
                        }
                   }
            );
        }
    );
});
</script>
<style type="text/css">
body { background: silver; }
.fail { color: red; }
.success { color: green; }
form.overwrite { display: inline; }
</style>
<body>
{{{top_links}}} {{!! html}}
<br />
<div>{{failed_count}} of {{tests_count}} tests failed</div>
<br />
<table>
    {{#tests}}
    <tr>
        <td><a href="#{{number}}" id="lnk_{{number}}">{{title}}</a></td>
        {{#succeeded}}
        <td class="success">SUCCEEDED</td>
        {{/succeeded}}
        {{^succeeded}}
        <td class="fail">FAILED</td>
        {{/succeeded}}
    </tr>
    {{/tests}}
</table>
<br />
{{#tests}}
<div>
    <h2>
        <a href="#lnk_{{number}}">^</a><a name="{{number}}"> {{title}} </a>
        {{^succeeded}}
        <span class="fail">FAILED</span>
        <form class="overwrite" action="{{title}}"><input type="submit" value="Overwrite" /></form>
        {{/succeeded}}
    </h2>
    <img src="images/{{image_old}}" alt="Old" />
    <a href="#{{number}}"><img class="rollover" src="images/{{image_new}}" alt="New" /></a>
    <br />
    <img src="images/{{image_diff}}" alt="Diff" />
</div>
{{/tests}}
</body>
''')

TOP_LINKS_SHOW_ALL = '<div><h1>Failed tests</h1><a href="all">SHOW ALL TESTS</a></div>'
TOP_LINKS_SHOW_FAILED  = '<div><h1>All tests</h1><a href="failed">SHOW FAILED TESTS ONLY</a></div>'


def img_compare(img, img2, img_diff):
    '''Saves img and img2 diff int img_diff. Returns true if images are equal.'''

    try:
        output_pipe = subprocess.Popen(['compare', '-metric', 'PSNR', img, img2, img_diff],
                                         stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    except OSError as e:
        print '\nMake sure that ImageMagick is installed and compare utility path is in your environment\n'
        raise e

    output = output_pipe.communicate()[0].strip()
    return output in ['inf', '1.#INF']


def generate_test_results():
    gen = itertools.count(1)

    print 'Generating report..'
    tests = []
    for root, dirs, files in os.walk('.'):
        sshot = os.path.join(root, 'screenshot.png')
        if not os.path.exists(sshot):
            continue
        print root
        test = {}
        test['number'] = gen.next()
        test['title'] = root
        test['image_old'] = sshot
        test['image_new'] = os.path.join(root, 'screenshot_new.png')
        test['image_diff'] = os.path.join(root, 'diff.png')
        succeeded = img_compare(test['image_old'], test['image_new'], test['image_diff'])
        if not succeeded:
            print 'FAILED'
        test['succeeded'] = succeeded
        tests.append(test)
    return tests


if __name__ == "__main__":
    try:
        import bottle
    except ImportError:
        raise ModuleRequiredError('bottle')
    from bottle import Bottle, request, response, debug, run, static_file
    import shutil

    tests = generate_test_results();
    failed_tests = [t for t in tests if not t['succeeded']]

    app = Bottle()

    @app.get('/all')
    def report():
        return pystache.render(TEMPLATE, {'tests': tests,
                                          'failed_count': len(failed_tests),
                                          'tests_count': len(tests),
                                          'top_links': TOP_LINKS_SHOW_FAILED})

    @app.get('/')
    @app.get('/failed')
    def failed():
        return pystache.render(TEMPLATE, {'tests': failed_tests,
                                          'failed_count': len(failed_tests),
                                          'tests_count': len(tests),
                                          'top_links': TOP_LINKS_SHOW_ALL})

    @app.get('/images/<path:re:.*\.png>')
    def img(path):
        return static_file(path, root='.', mimetype='image/png')

    @app.post('/overwrite')
    def update_sshot():
        try:
            test_dir = request.POST['dir']
            print 'Overwriting screenshots in "%s"' % (test_dir)
            shutil.copy(os.path.join(test_dir, 'screenshot_new.png'),
                        os.path.join(test_dir, 'screenshot.png'))
        except Exception, e:
            print "Failed\n", e
            #response.status = 500
            return { 'status': 'failed', 'error': str(e) }
        return { 'status': 'succeeded' }


    debug(True)
    run(app, host='0.0.0.0', port=8071, reloader=True)

