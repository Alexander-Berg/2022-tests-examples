
import sys
import urllib2
import json

address = sys.argv[1]
mode = sys.argv[2]

fields = {
    'prefix': 10,
    'action': 'delete',
    'request': 'something'
    }

if mode == 'badrequest':
    fields['action'] = [55, 'rubbish']
    fields['prefix'] = 'more rubbish'
elif mode == 'mix_body_children':
    fields = {
        "action": "add",
        "prefix": 0,
        "docs": [{
            "options": {"language": "rus", "charset": "UTF8", "mime_type": "text/html"},
            "mid": [{"value": 10000000000, "type": "#g"}],
            "url": "123456790_1",
            "body": {"value": "body_text",
                     "children": {"hhh": {"type": "#z", "value": "child1_text"}}
                     },
            "test": [{"value": "test@test", "type": "#l"}]
        }]
    }

print 'start test, mode %s' % mode
try:
    req = urllib2.Request('http://' + address)
    req.add_header('Content-Type', 'text/json')
    print 'sending request...'
    try:
        if mode == 'incorrect_json':
            text = json.dumps(fields)
            text = text.replace('something', 'something"')
            response = urllib2.urlopen(req, text)
        else:
            response = urllib2.urlopen(req, json.dumps(fields))
        code = response.getcode()
        print 'received code %s' % code
        sys.exit(code)
    except urllib2.HTTPError as e:
        print 'received code %s' % e.code
        ans = e.read()
        print ans
        if e.code == 400:
            sys.exit(0)
        else:
            sys.exit(1)
except Exception as e:
    print e
    sys.exit(1)
