import os
import gevent
from cStringIO import StringIO

from infra.dist.dmover.lib.internal.package import Package

from repo_index import _RepoIndex, DebianRepoIndex, RedhatRepoIndex

from gevent.lock import BoundedSemaphore
from collections import defaultdict


class System(object):

    def __init__(self, root):
        self.root = os.path.normpath(root)
        self.files = {}
        self.repos = {}
        self.dmove_keys = {}
        self.remove_delay = 2.0
        self.appear_delay = 0.2

    def add_file(self, path, content):
        path = os.path.normpath(path)
        self.files[path] = content

    def open(self, path):
        path = os.path.normpath(path)
        if path in self.files:
            return StringIO(self.files[path])
        else:
            raise IOError(2, 'No such file or directory')

    def isdir(self, path):
        path = os.path.normpath(path)

        if not path.startswith(self.root):
            return False

        if path == self.root:
            return True

        path = os.path.relpath(path, self.root)
        dirs = path.split('/')

        current = self.repos
        for dir in dirs:
            if not isinstance(current, dict) or dir not in current:
                break
            else:
                current = current[dir]
        else:
            return True

        return False

    def get_repo(self, name, arch, branch=None, repo_class=_RepoIndex, dmove_key=None, **kwargs):
        self.repos.setdefault(name, {})

        if branch:
            self.repos[name].setdefault(branch, {})
            path = self.repos[name][branch]
        else:
            path = self.repos[name]

        if arch not in path:
            repo = repo_class(**kwargs)
            path[arch] = repo
        else:
            repo = path[arch]

        if dmove_key:
            self.dmove_keys[dmove_key] = name

        return repo

    def get_debian_repo(self, name, arch, branch, dmove_key=None):
        return self.get_repo(name, arch, branch, repo_class=DebianRepoIndex, dmove_key=dmove_key)

    def get_redhat_repo(self, name, arch, branch=None, dmove_key=None):
        return self.get_repo(name, arch, branch, repo_class=RedhatRepoIndex, dmove_key=dmove_key)

    def debian_dmove(self, dmove_key, to_branch, location, skip_reindex=False):
        if dmove_key not in self.dmove_keys:
            return 1, 'Unknown dmove_key'

        name = self.dmove_keys[dmove_key]
        if name not in self.repos or location.branch not in self.repos[name]:
            return 1, 'Repo or branch not found'

        arches = []
        for arch, repo in self.repos[name][location.branch].iteritems():
            if repo.has_package(location.package.name, location.package.version):
                arches.append(arch)

        if not arches:
            return 1, 'Package was not found in any arch'
        else:
            for arch in arches:
                retcode, output = self.dmove(
                    name,
                    location.branch,
                    to_branch,
                    arch,
                    location.package.name,
                    location.package.version
                )

            return retcode, output

    def redhat_dmove(self, dmove_key, to_branch, location, skip_reindex=False):

        if dmove_key not in self.dmove_keys:
            return 1, 'Unknown dmove_key'

        name = self.dmove_keys[dmove_key]
        if name not in self.repos:
            return 1, 'Repo not found'
        elif location.branch not in self.repos[name]:
            return 1, 'Branch not found in repo'
        elif location.arch not in self.repos[name][location.branch]:
            return 1, 'Arch not found in repo branch'

        return self.dmove(
            name,
            location.branch,
            to_branch,
            location.arch,
            location.package.name,
            location.package.version,
            location.package_arch
        )

    def dmove(self, name, from_branch, to_branch, arch, package, version, package_arch=None):
        from_repo = self.get_repo(name=name, branch=from_branch, arch=arch)

        def remove(sleep):
            gevent.sleep(sleep)
            self.build_index(name, from_branch)
            print "Removed %s" % package

        def appear(sleep):
            gevent.sleep(sleep)
            self.build_index(name, to_branch)
            print "Added %s" % package

        self.get_repo(
            name=name,
            branch=to_branch,
            arch=arch,
            repo_class=from_repo.__class__
        ).add_package(Package(package, version, None), arch=package_arch)

        from_repo.remove_package(Package(package, version, None), arch=package_arch)

        gevent.spawn(remove, self.remove_delay)
        gevent.spawn(appear, self.appear_delay)

        return 0, 'All is ok'

    def build_index(self, name, branch):
        archs = self.repos[name][branch]
        if isinstance(archs, dict):
            for arch, repo in archs.iteritems():
                self.add_file(
                    os.path.join(self.root, repo.index_path(name=name, arch=arch, branch=branch)),
                    repo.make_file()
                )
        else:
            repo = archs
            arch = branch
            self.add_file(
                os.path.join(self.root, repo.index_path(name=name, arch=arch, branch=None)),
                repo.make_file()
            )

    def build_indexes(self):
        for name, branches in self.repos.iteritems():
            for branch, archs in branches.iteritems():
                self.build_index(name, branch)


class FakeLocker(object):

    class _SourcePackageLocker(object):

        def __init__(self, locker, source_packages):
            self.locker = locker
            self.source_packages = source_packages

        def __enter__(self):
            # locations are made unique and sorted before locking to avoid deadlocks
            for source_package in sorted(set(self.source_packages)):
                self.locker._source_package_locks[source_package].acquire()

            return self

        def __exit__(self, exc_type, exc_val, exc_tb):
            for source in set(self.source_packages):
                self.locker._source_package_locks[source].release()

    def __init__(self, _):
        # key is tuple(repo, source_package, version)
        self._source_package_locks = defaultdict(BoundedSemaphore)

    def source_package_lock(self, source_packages):
        return self._SourcePackageLocker(self, source_packages)
