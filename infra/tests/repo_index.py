import gzip
import os
from cStringIO import StringIO
from hashlib import sha1


class _RepoIndex(object):

    ARCHITECTURES = []
    INDEX_PATH = 'no_index_here'

    class _Package(object):

        def __init__(self, name, version, arch):
            self.name = name
            self.version = version
            self.arch = arch

        def __hash__(self):
            return hash((self.name, self.version, self.arch))

        def __eq__(self, other):
            return self.__hash__() == other.__hash__()

    def __init__(self):
        self.packages = set()

    def add_package(self, package, arch=None):
        if arch is None:
            arch = 'i386'
        self.packages.add(self._Package(package.name, package.version, arch=arch))

    def remove_package(self, package, arch=None):
        if arch is None:
            arches = self.ARCHITECTURES
        else:
            arches = [arch]
        for arch in arches:
            self.packages.discard(self._Package(package.name, package.version, arch=arch))

    def has_package(self, package, version, arch=None):
        if arch is None:
            arches = self.ARCHITECTURES
        else:
            arches = [arch]

        for arch in arches:
            if self._Package(package, version, arch) in self.packages:
                return True

        return False


class DebianRepoIndex(_RepoIndex):

    ARCHITECTURES = ['all', 'i386', 'amd64']

    @staticmethod
    def index_path(name, arch, branch):
        path_items = [name]
        if branch:
            path_items.append(branch)
        path_items += [arch, 'Packages']
        return os.path.join(*path_items)

    def make_file(self):
        packages_string = ''
        for package in self.packages:
            packages_string += 'Package: %s\n' % package.name
            packages_string += 'Version: %s\n' % package.version
            packages_string += 'Architecture: %s\n' % package.arch
            packages_string += '\n'
        return packages_string


class RedhatRepoIndex(_RepoIndex):

    ARCHITECTURES = ['noarch', 'i386', 'x86_64']
    INDEX_PATH = 'repodata/other.xml.gz'

    def __init__(self, redhat_version=5):
        self.version = redhat_version
        super(RedhatRepoIndex, self).__init__()

    def index_path(self, name, arch, branch):
        path_items = [name]
        if branch:
            path_items.append(branch)
        path_items.append(str(self.version))
        path_items += [arch, 'repodata', 'other.xml.gz']
        return os.path.join(*path_items)

    def make_file(self):
        other = StringIO()
        gzip_file = gzip.GzipFile(fileobj=other, mode='w')
        gzip_file.write('<?xml version="1.0" encoding="UTF-8"?>\n')
        gzip_file.write('<otherdata xmlns="http://linux.duke.edu/metadata/other" packages="%d">\n' % len(self.packages))

        for package in self.packages:
            package_hash = sha1('%s_%s_%s' % (package.name, package.version, package.arch)).digest().encode('hex')
            gzip_file.write('<package pkgid="%s" name="%s" arch="%s">\n' % (package_hash, package.name, package.arch))

            if '-' in package.version:
                version, release = package.version.split('-', 1)
            else:
                version = package.version
                release = ''
            gzip_file.write('<version epoch="0" ver="%s" rel="%s"/>\n' % (version, release))

            gzip_file.write('</package>\n')

        gzip_file.write('</otherdata>\n')
        gzip_file.close()

        return other.getvalue()
