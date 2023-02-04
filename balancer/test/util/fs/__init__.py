# -*- coding: utf-8 -*-
import os
import shutil
import stat


class FileSystemException(Exception):
    pass


def abs_path(path, cwd=None):
    if cwd is None:
        cwd = os.getcwd()
    return os.path.abspath(os.path.join(cwd, os.path.expanduser(path)))


class FileSystemManager(object):
    def __init__(self, root_dir):
        super(FileSystemManager, self).__init__()
        self.__root_dir = root_dir
        self.__reserved_paths = set()

    @property
    def root_dir(self):
        return self.__root_dir

    def in_root_dir(self, path):
        return os.path.commonprefix([self.__root_dir, path]) == self.__root_dir

    def abs_path(self, name):
        """
        :returns: absoulte file path, relative to fs manager root dir
        :rtype: str
        """
        return os.path.join(self.__root_dir, os.path.expanduser(name))

    def __check_in_root_dir(self, path):
        if not self.in_root_dir(path):
            raise FileSystemException('path "{}" not in root directory "{}"'.format(path, self.__root_dir))

    def get_unique_name(self, base_name):
        base_path = os.path.join(self.__root_dir, base_name)
        path = base_path
        cnt = 0
        while os.path.exists(path) or path in self.__reserved_paths:
            cnt += 1
            path = '%s.%d' % (base_path, cnt)
        self.__reserved_paths.add(path)
        return path

    def create_file(self, base_name):
        """
        :return: absolute path to created file
        :rtype: str
        """
        if os.path.isabs(base_name):
            path = base_name
        else:
            path = self.get_unique_name(base_name)
        parent_dir = os.path.dirname(path)
        if not os.path.exists(parent_dir):
            os.makedirs(parent_dir)
        with open(path, 'w'):
            pass
        self.chmod_rw(path)
        return path

    def create_dir(self, base_name):
        """
        :return: absolute path to created directory
        :rtype: str
        """
        path = self.get_unique_name(base_name)
        os.makedirs(path)
        return path

    def remove(self, path):
        path = self.abs_path(path)
        self.__check_in_root_dir(path)
        if os.path.exists(path):
            if os.path.isfile(path):
                self.chmod_rw(path)
                os.remove(path)
            else:
                for root, dirs, files in os.walk(path):
                    for dir_ in dirs:
                        self.chmod_rwx(os.path.join(root, dir_))
                    for file_ in files:
                        self.chmod_rw(os.path.join(root, file_))
                shutil.rmtree(path)

    def copy(self, src, dst):
        src = self.abs_path(src)
        dst = self.abs_path(dst)
        self.remove(dst)
        if os.path.isfile(src):
            shutil.copyfile(src, dst)
        else:
            shutil.copytree(src, dst)
        return dst

    def chmod(self, path, mode):
        path = self.abs_path(path)
        self.__check_in_root_dir(path)
        os.chmod(path, mode)

    def chmod_rw(self, path):
        self.chmod(path, stat.S_IRUSR | stat.S_IWUSR | stat.S_IRGRP | stat.S_IWGRP | stat.S_IROTH | stat.S_IWOTH)

    def chmod_rwx(self, path):
        self.chmod(path, stat.S_IRWXU | stat.S_IRWXG | stat.S_IRWXO)

    def chmod_default(self, path):
        path = self.abs_path(path)
        if os.path.isfile(path):
            self.chmod(path, stat.S_IRUSR | stat.S_IWUSR | stat.S_IRGRP | stat.S_IROTH)
        else:
            self.chmod(path, stat.S_IRWXU | stat.S_IRGRP | stat.S_IXGRP | stat.S_IROTH | stat.S_IXOTH)

    def chmod_default_recursive(self, path):
        self.__chmod_default_recursive(path, list())

    def __chmod_default_recursive(self, path, done):
        path = self.abs_path(path)
        if path not in done:
            done.append(path)
            if os.path.isfile(path):
                self.chmod_default(path)
            else:
                for root, dirs, files in os.walk(path):
                    for dir_ in dirs:
                        abs_dir_path = os.path.join(root, dir_)
                        self.chmod_default(abs_dir_path)
                        self.__chmod_default_recursive(abs_dir_path, done)
                    for file_ in files:
                        self.chmod_default(os.path.join(root, file_))

    def read_file(self, path):
        """Read file contents

        :param str path: absolute file path or path, relative to fs manager root dir
        :rtype: str
        """
        path = self.abs_path(path)
        with open(path) as file_:
            return file_.read()

    def read_lines(self, path):
        path = self.abs_path(path)
        with open(path, 'r') as file_:
            return file_.readlines()

    def rewrite(self, path, data, chmod=False):
        path = self.abs_path(path)
        self.__check_in_root_dir(path)
        if chmod and not os.access(path, os.W_OK):
            path_stat = os.stat(path)
            self.chmod(path, path_stat.st_mode | stat.S_IWUSR | stat.S_IWGRP | stat.S_IWOTH)
        with open(path + ".tmp", 'w') as file_:
            file_.write(data)
        os.rename(path + ".tmp", path)
