import binascii
import collections
import difflib
import inspect
import itertools
import json
import os.path

import pytest

import yatest.common

DATA_FILE = 'maps/infra/sedem/lib/deprecated_fancy_tree/tests/golden.data.txt'

HTML_TEMPLATE_START = """
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
<head>
<meta charset="UTF-8">

<link rel="stylesheet" href="https://www.w3schools.com/w3css/4/w3.css">
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.7.0/css/font-awesome.min.css">

<style>
.orange_block {{
    color: orange;
    width: 20px;
}}

.flash {{
    animation:fading 3s 1 paused;
    opacity: 0;
}}
@keyframes fading {{
    0%{{opacity:0}}
    50%{{opacity:1}}
    100%{{opacity:0}}
}}

table.diff {{
    font-family:Courier;
    border:medium;
}}

.diff_header {{
    background-color:#e0e0e0;
}}

td.diff_header {{
    text-align:right;
}}

.diff_next {{
    background-color:#c0c0c0;
}}

.diff_add {{
    background-color:#aaffaa;
}}

.diff_chg {{
    background-color:#ffff77;
}}

.diff_sub {{
    background-color:#ffaaaa;
}}

</style>

<script>
'use strict';

var data_def = {json_definition};
var separator = '{section_separator}';

function open_section(idx)
{{
    var x = document.getElementById('Section' + idx);
    x.classList.toggle("w3-show");
    x.previousElementSibling.classList.toggle("w3-dark-grey");
}}

function assign_choose(idx, accept)
{{
    data_def[idx].T = accept ? '+': '-';
    var x = document.getElementById('Indicator' + idx);
    x.innerHTML = accept ? '&#xf00c;' : '&#xf00d;';
    x.style.color = accept ? 'green': 'red';
    open_section(idx);

    var total = data_def.length;
    ++idx;
    for(var i=0; i<total; ++idx, ++i)
    {{
        if (idx >= total) idx=0;
        var x = document.getElementById('Section' + idx);
        if (x)
        {{
            if (data_def[idx].T == '?')
            {{
                if (!x.classList.contains('w3-show')) open_section(idx);
                return;
            }}
            if (x.classList.contains('w3-show')) open_section(idx);
        }}
    }}

    // Show result
    var result = separator + '\\n';
    for(var i=0; i<data_def.length; ++i)
    {{
        var x = data_def[i];
        var text = x.T == '+' ? x.N : x.O;
        if (typeof(text) != 'undefined')
        {{
            result += x.I+'\\n\\n'+text+separator + '\\n';
        }}
    }}
    document.getElementById('ResultText').value = result;
    document.getElementById('Result').classList.add('w3-show');
    document.getElementById('ResultText').select();
    if (document.execCommand && document.execCommand("copy"))
    {{
        var x = document.getElementById('Message');
        x.style.animation = 'none';
        x.offsetHeight; // trigger reflow
        x.style.animation = null; // This will reset animation to value in CSS. Animation state also will be reseted
        x.style.animationPlayState = 'running';
    }}
}}
</script>
</head>
<body>
"""

HTML_TEMPLATE_BODY = """
<div onclick="open_section({index})" class="w3-button w3-light-grey w3-block w3-left-align" title="Section was {section_status.message}">
 <span class="fa w3-large w3-center orange_block" id="Indicator{index}">&#xf128;</span>
 <b class="w3-tag w3-round-large w3-{section_status.color_tag}">&nbsp;{section_status.letter}&nbsp;</b>
{section_title}
</div>
<div id="Section{index}" class="w3-hide w3-margin-bottom w3-container">
 <div class="w3-container w3-leftbar w3-{section_status.color_body}">
{body}
 </div>
 <div class="w3-bar w3-light-gray w3-display-container w3-border-top">
 <button class="w3-button w3-ripple w3-hover-green w3-pale-green w3-display-middle" onclick="assign_choose({index},true)">Accept changes ({section_status.message_accept})</button>
 <button class="w3-button w3-ripple w3-hover-red w3-pale-red w3-right" onclick="assign_choose({index},false)">Reject changes ({section_status.message_reject})</button>
</div></div>
"""

HTML_FOOTER = """
<div id="Result" class="w3-hide w3-border w3-panel">
 Save this text to file <b>golden.data.txt</b> in <i>tests</i> directory
 <span id="Message" class="flash"> <b>(text copied to clipboard)</b></span><br>
 <textarea id="ResultText" readonly="true" style="width:100%" rows="40"></textarea>
</div>

</body>
</html>
"""

SectionStatus = collections.namedtuple('SectionStatus', ('message', 'letter', 'color_tag', 'color_body', 'message_accept', 'message_reject'))

# Constants for tests status (also contains data for HTML formatting)
STATUS_MODIFIED = SectionStatus('modified', 'M', 'amber', 'sand', 'New content valid', 'New content wrong')
STATUS_ADDED = SectionStatus('added', 'A', 'green', 'pale-green', 'Add section', "Don't add section")
STATUS_REMOVED = SectionStatus('removed', 'D', 'red', 'pale-red', 'Remove section', "Don't remove section")
STATUS_NOT_MODIFIED = None


class GoldDataManager:
    """Main test data manager"""

    class TestData(object):
        """Info for 1 test"""
        def __init__(self, org, new, name):
            self.org = org
            self.new = new
            self.name = name

        def status(self):
            """get modification status"""
            if self.org is None:
                return STATUS_ADDED
            if self.new is None:
                return STATUS_REMOVED
            if self.org == self.new:
                return STATUS_NOT_MODIFIED
            return STATUS_MODIFIED

        def json_part(self):
            """get part of JSON description (for HTML page)"""
            result = {'T': '-' if self.status() is STATUS_NOT_MODIFIED else '?', 'I': self.name}
            if self.org is not None:
                result['O'] = self.org
            if self.new is not None and self.org != self.new:
                result['N'] = self.new
            return result

        def html_body(self, index):
            status = self.status()
            if status is STATUS_NOT_MODIFIED:
                return None
            if status is STATUS_MODIFIED:
                lines_b = self.new.splitlines()
                lines_a = self.org.splitlines()
                body = difflib.HtmlDiff().make_table(lines_a, lines_b)
            else:
                body = "<pre>" + (self.org or self.new) + "</pre>"
            subst_dict = {
                'index': index,
                'section_status': status,
                'section_title': self.name,  # !!! Add HTML escapes
                'body': body
            }
            return HTML_TEMPLATE_BODY.format(**subst_dict)

        def text_body(self):
            if self.new is None:
                return "Removed dataset '" + self.name + "'found\n"
            if self.org is None:
                return "New dataset '" + self.name + "'found. Contents follow\n" + self.new
            if self.new == self.org:
                return ''
            result = ["Dataset '" + self.name + "' modified.\n"]
            lines_b = self.new.splitlines()
            lines_a = self.org.splitlines()
            for line in itertools.islice(difflib.unified_diff(lines_a, lines_b), 2, None):
                result.append('  ' + line + '\n')
            return ''.join(result)

    def __init__(self):
        self.org_data = collections.OrderedDict()
        self.start_marker = ('-' * 40) + binascii.hexlify(os.urandom(32)).hex() + '\n'
        real_data_file = yatest.common.source_path(DATA_FILE)
        if os.path.isfile(real_data_file):
            with open(real_data_file, 'rU') as data_file:
                self.start_marker = next(data_file)
                try:
                    while True:
                        lines = []
                        obj_name = next(data_file).rstrip('\n')
                        assert next(data_file) == '\n', DATA_FILE + 'file format wrong: Empty line expected between ' \
                                                                    'Check Tag and body '
                        while True:
                            line = next(data_file)
                            if line == self.start_marker:
                                break
                            lines.append(line)
                        self.org_data[obj_name] = self.TestData(''.join(lines), None, obj_name)
                except StopIteration:
                    pass

    def check_data(self, obj_name, new_data):
        """Main method for test output check."""
        if not new_data.endswith('\n'):
            new_data += '\n'
        if obj_name not in self.org_data:
            self.org_data[obj_name] = self.TestData(None, new_data, obj_name)
            raise Exception('No data registered for "{}"'.format(obj_name))
        data = self.org_data[obj_name]
        assert data.new is None, "Duplicated test found for "+obj_name
        data.new = new_data
        if new_data != data.org:
            lines = itertools.islice(difflib.unified_diff(data.org.splitlines(), data.new.splitlines()), 2, None)
            raise Exception('Data mismatch for "{}"\n{}'.format(obj_name, '\n'.join(lines)))

    def save_results(self):
        """Save output results as HTML and plain text diff (2 files generated)"""
        with open(yatest.common.output_path('data.diff.txt'), 'w') as txt_file:
            for data in self.org_data.values():
                txt_file.write(data.text_body())

        with open(yatest.common.output_path('data.diff.html'), 'w') as html_file:
            json_struct = [data.json_part() for data in list(self.org_data.values())]
            subst_vars = {
                'json_definition': json.dumps(json_struct),
                'section_separator': self.start_marker.rstrip('\n')
            }
            html_file.write(HTML_TEMPLATE_START.format(**subst_vars))
            for index, data in enumerate(self.org_data.values()):
                html_body = data.html_body(index)
                if html_body:
                    html_file.write(html_body)
            html_file.write(HTML_FOOTER)


@pytest.mark.usefixtures('data_manager')  # This fixture should be placed in 'conftest.py' file
class TestBase:
    def check(self, data):
        name = '<unknown>'
        frame = inspect.currentframe()
        while frame:
            if frame.f_code.co_name.startswith('test_'):
                name = frame.f_code.co_name
                break
            frame = frame.f_back
        obj_name = self.__class__.__name__ + '.' + name
        GoldDataManager.root.check_data(obj_name, data)  # GoldDataManager.root created in texture 'data_manager'
