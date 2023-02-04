from __future__ import absolute_import
import os
import sys
import types
import importlib


class LibraryProxyLoader(object):
    def __init__(self, from_root, to_root):
        self.from_root = from_root
        self.to_root = to_root

    def load_original_module(self, fullname):
        name = self.from_root + fullname[len(self.to_root):]
        mod = importlib.import_module(name)
        return mod

    def load_module(self, fullname):
        if fullname in sys.modules:
            return sys.modules[fullname]

        name = self.to_root + fullname[len(self.from_root):]
        mod = importlib.import_module(name)
        sys.modules[fullname] = mod
        return mod


class LibraryModProxy(types.ModuleType):
    def __init__(self, name, from_root, to_root):
        super(LibraryModProxy, self).__init__(name)
        self.__loader__ = LibraryProxyLoader(from_root, to_root)

    def __getattr__(self, name):
        mod = self.__loader__.load_original_module(self.__name__)
        return getattr(mod, name)

    def __dir__(self):
        mod = self.__loader__.load_module(self.__name__)
        return dir(mod)


def make_stub(modname):
    proxy = types.ModuleType(modname, "STUB")
    if '.' in modname:
        parent, shortname = modname.rsplit('.', 1)
        setattr(importlib.import_module(parent), shortname, proxy)

    sys.modules[modname] = proxy


def make_proxy(from_root, to_root, modname):
    m = modname[:-9] if modname.endswith('.__init__') else modname
    if m == from_root or m.startswith(from_root + '.'):
        proxyname = to_root + m[len(from_root):]
        proxy = LibraryModProxy(proxyname, from_root, to_root)
        if '.' in proxyname:
            parent, shortname = proxyname.rsplit('.', 1)
            setattr(importlib.import_module(parent), shortname, proxy)

        sys.modules[proxyname] = proxy


def make_proxy_arcadia(loader, from_root, to_root):
    for modname in sorted(list(globals()['__loader__'].memory)):
        make_proxy(from_root, to_root, modname)


def _make_modname(filename):
    if filename.endswith('.pyc'):
        filename = filename[:-4]
    elif filename.endswith('.py'):
        filename = filename[:-3]
    else:
        return

    modname = filename.split(os.path.sep)
    if modname[-1] == '__init__':
        modname.pop()

    return '.'.join(modname)


def make_proxy_egg(loader, from_root, to_root):
    for modname in sorted(set(filter(None, map(_make_modname, globals()['__loader__']._files.keys())))):
        make_proxy(from_root, to_root, modname)


def patch_modules():
    if getattr(patch_modules, 'patched', False):
        return

    patch_modules.patched = True

    standalone = getattr(sys, 'is_standalone_binary', False)

    loader = globals().get('__loader__')
    if loader is None:
        return
    elif standalone:
        make_proxy_arcadia(loader, 'kernel.util', 'ya.skynet.util')
        make_proxy_arcadia(loader, 'library', 'ya.skynet.library')
        return

    from zipimport import zipimporter
    if isinstance(loader, zipimporter):
        make_stub('kernel')
        make_proxy_egg(loader, 'ya.skynet.util', 'kernel.util')
        make_proxy_egg(loader, 'ya.skynet.library', 'library')
