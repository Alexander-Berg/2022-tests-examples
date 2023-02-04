#!/usr/bin/env python
import urllib2
import xmlrpclib
import subprocess
import os
import time
import stat
import sys
import json
import shutil
import hashlib

DISTR_RESOURCE_TYPE = 'FUSIONSTORE_PACKAGE'
DISTR_RESOURCE_ATTRS = {'resource_name': 'fusionstore', 'platform': 'precise'}

SANDBOX_ADDR = 'https://sandbox.yandex-team.ru/sandbox/xmlrpc'
SB_CHECK_INTERVAL = 3600 * 24

RES_FOLDER_NAME = 'resource_cache'
TAR_EXT = ('.tar.gz', '.tgz', '.tar.gzip')

DISTRIB_RES_FILTER = {'resource_type': DISTR_RESOURCE_TYPE,
                      'all_attrs': DISTR_RESOURCE_ATTRS}
DOLBILO_RES_FILTER = {'resource_type': 'DEXECUTOR_EXECUTABLE',
                      'all_attrs': {'rtyserver': 'yes'}}

KEYWORDS = {'distributor': DISTRIB_RES_FILTER,
            'dolbilo': DOLBILO_RES_FILTER}


def get_resource_data(filt):
    if not 'resource_type' in filt and not 'id' in filt:
        raise Exception('id or resource_type must be in filter')
    serv = xmlrpclib.ServerProxy(SANDBOX_ADDR)
    filt.update({'state': 'READY', 'arch': 'linux', 'limit': 1})
    print('finding resources, filter %s' % filt)
    res = serv.listResources(filt)
    if not res:
        return {'error': 'no resources found'}
    if not res[0]['skynet_id'] or not res[0]['id']:
        raise Exception('something wrong with answer: %s' % res[0])
    return res[0]

def dest_name(src):
    dest = src
    for ext in TAR_EXT:
        dest = dest.replace(ext, '_unpacked')
    return dest

def untar(fpath, dest=None):
    if not dest:
        dest = dest_name(fpath)
        print('dest for tar: %s' % dest)
    if not os.path.exists(dest):
        os.mkdir(dest)
    cmd = ['tar', 'xvf', fpath, '-C', dest]
    with open('untar.' + os.path.basename(dest) +'.out', 'w') as out:
        p = subprocess.Popen(cmd, stdout=out, stderr=out)
        p.wait()
    if p.returncode:
        raise Exception('problems with untar')
    d = os.listdir(dest)
    if len(d) == 1:
        dest = os.path.join(dest, d[0])
    return dest

def download_rbtorrent(skynet_id, dest_dir):
    if os.path.exists(dest_dir):
        shutil.rmtree(dest_dir)
    os.mkdir(dest_dir)
    cmd = ['sky', 'get', '-N', 'Backbone', '-wu', skynet_id]
    p = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, cwd=dest_dir)
    out, err = p.communicate()
    files = os.listdir(dest_dir)
    if p.returncode or len(files) == 0:
        raise Exception('error in sky get, returncode %s, files in dir: %s' % (p.returncode, files))
    if len(files) > 1:
        return dest_dir
    else:
        return os.path.join(dest_dir, files[0])

def make_resource_filter(resource_filter):
    filt = {}
    if 'id' in resource_filter:
        r_sign = str(resource_filter['id'])
        filt['id'] = id
    elif 'attrs' in resource_filter and 'type' in resource_filter:
        r_sign = resource_filter['type'] + '_' \
                 + hashlib.md5('_'.join(sorted(resource_filter['attrs'].values()))).hexdigest()
        filt['all_attrs'] = resource_filter['attrs']
        filt['resource_type'] = resource_filter['type']
    elif 'type' in resource_filter:
        r_sign = resource_filter['type']
        filt['resource_type'] = resource_filter['type']
    elif 'keyword' in resource_filter:
        kword = resource_filter['keyword']
        r_sign = 'K_' + resource_filter['keyword']
        if kword in KEYWORDS:
            filt = KEYWORDS[kword]
    else:
        return None
    return {'filter' : filt, 'sign': r_sign}

def get_resource(resource_filter):
    local_dir = os.path.join(os.getcwd(), RES_FOLDER_NAME)
    if not os.path.exists(local_dir):
        os.mkdir(local_dir)
    caches = os.listdir(local_dir)

    res_params = make_resource_filter(resource_filter)
    if not res_params:
        print('no good params in %s' % resource_filter)
        raise Exception('incorrect resource filter')
    r_sign = res_params['sign']
    r_filt = res_params['filter']

    suit_names = [f for f in caches if f.startswith(r_sign) or f.endswith(r_sign)]
    now = int(time.time())
    print('cache: %s possible dirs found' % len(suit_names))
    for f in suit_names:
        ftime = os.stat(os.path.join(local_dir, f))[stat.ST_MTIME]
        if (now - ftime) < SB_CHECK_INTERVAL:
            pth = os.path.join(local_dir, f)
            unpacked_files = [p for p in os.listdir(pth) if p.endswith('_unpacked')]
            if len(unpacked_files) == 1:
                pth = os.path.join(pth, unpacked_files[0])
            if os.path.isdir(pth) and len(os.listdir(pth)) == 1:
                pth = os.path.join(pth, os.listdir(pth)[0])
            if pth.endswith(TAR_EXT):
                print('untar %s...' % pth)
                pth = untar(pth)
                print('untar...OK')
            print('found in cache, path %s' % pth)
            return {'result': pth,
                    'debug_info': 'existing_path'}

    print('not found in cache, try to load %s...' % r_filt)
    resource = get_resource_data(r_filt)
    if 'no resource' in resource.get('error', ''):
        if 'from_path' in r_filt.get('all_attrs', {}) and r_filt['all_attrs']['from_path'].startswith('rbtorrent:'):
            skynet_id = r_filt['all_attrs']['from_path']
            resource_id = 0
        else:
            raise Exception('no resources found')
        print('trying to download by skynet id %s' % skynet_id)
    else:
        print('found resource %s, loading...' % resource)
        skynet_id = resource['skynet_id']
        resource_id = resource['id']
    folder_for_res = os.path.join(local_dir, str(resource_id) + '_' + r_sign)
    local_res_path = download_rbtorrent(skynet_id, folder_for_res)
    print('loaded, path %s' % local_res_path)
    if local_res_path.endswith(TAR_EXT):
        print('untar %s...' % local_res_path)
        local_res_path = untar(local_res_path)
        print('untar...OK')
    return {'result': local_res_path}


if __name__ == '__main__':
    # usage:
    #get_resources.py cluster.cfg nodeName output_file.json
    cluster_conf = sys.argv[1]
    node_name = sys.argv[2]
    output = sys.argv[3]
    print('command: %s' % sys.argv)
    if not os.path.exists(cluster_conf):
        print('file %s does not exist' % cluster_conf)
        exit(1)
    with open(cluster_conf, 'r') as f:
        ftxt = f.read()
        js = ''.join([l for l in ftxt.splitlines() if not l.strip().startswith('//')])
        cluster = json.loads(js)
    nodes = [nd for nd in cluster if nd.get('name', '')==node_name]
    if len(nodes) == 0 or len(nodes) > 1:
        print('%s nodes with name %s, must be 1' % (len(nodes), node_name))
        exit(1)
    node = nodes[0]
    if 'vars' not in node:
        print('no vars, nothing to download')
        exit(0)
    ress = dict([
        (varname, vardescr) for varname, vardescr
        in node['vars'].items()
        if 'resource' in vardescr and isinstance(vardescr, dict)
    ])
    if len(ress) == 0:
        print('no resources in vars found')
        exit(0)
    result = {}
    print('%s resources to find' % len(ress))
    if not os.path.exists(os.path.join(os.getcwd(), 'root')):
        os.makedirs(os.path.join(os.getcwd(), 'root'))
    for var in ress:
        filt = ress[var]['resource']
        print('getting resource with %s...' % filt)
        resource_path = get_resource(filt)['result']
        print('resource got, cache path %s, copying...' % resource_path)

        dst = ress[var].get('destination', os.path.join(os.getcwd(), 'root', node_name + '.' + var))
        if os.path.exists(dst) and dst != resource_path:
            if os.path.isdir(dst):
                shutil.rmtree(dst)
            else:
                os.unlink(dst)

        if os.path.isdir(resource_path):
            shutil.copytree(resource_path, dst, symlinks=True)
        else:
            shutil.copy2(resource_path, dst)
        print('ultimate path %s' % dst)
        result[var] = dst

    fcont = json.dumps(result, indent=4)
    with open(output, 'w') as ou:
        ou.write(fcont)
