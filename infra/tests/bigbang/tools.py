from cStringIO import StringIO
import hashlib

from skybone.rbtorrent import bencode


def gen_resource(files={'testfile': {'size': 10, 'data': '\1'}}):
    structure, torrents = {}, {}

    for name, info in files.items():
        if 'data' in info:
            if len(info['data']) == 1:
                data = info['data'] * info['size']
            else:
                data = info['data']  # use as is
        else:
            data = open('/dev/urandom', 'rb').read(info['size'])

        executable = info.get('executable', False)

        structure[name] = {
            'type': 'file',
            'path': name,
            'md5sum': hashlib.md5(data).digest(),
            'size': info['size'],
            'executable': executable,
            'mode': 'rwxr-xr-x' if executable else 'rw-r--r--'
        }

        piece_size = 4 * 1024 * 1024
        sha1hashes = ''

        sdata = StringIO(data)

        while True:
            block = sdata.read(piece_size)
            if not block:
                break
            sha1hashes += hashlib.sha1(block).digest()

        ti = {
            'name': 'data',
            'piece length': piece_size,
            'pieces': sha1hashes,
            'length': info['size'],
        }

        ti_infohash = hashlib.sha1(bencode.bencode(ti)).hexdigest()

        structure[name]['resource'] = {'type': 'torrent', 'id': ti_infohash}
        torrents[ti_infohash] = {'info': ti}

    resource = {'structure': structure, 'torrents': torrents}
    resource_binary = bencode.bencode(resource)

    resource_binary_s = StringIO(resource_binary)
    resource_pieces = ''

    while True:
        data = resource_binary_s.read(piece_size)
        if not data:
            break
        resource_pieces += hashlib.sha1(data).digest()

    resource_ti = {
        'name': 'metadata',
        'piece length': piece_size,
        'pieces': resource_pieces,
        'length': len(resource_binary),
    }

    resource_infohash = hashlib.sha1(bencode.bencode(resource_ti)).hexdigest()

    return resource_infohash, resource
