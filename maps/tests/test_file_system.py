import os
import shutil

from library.python import resource
from maps.carparks.tools.cleaner.lib import lib


class TestFileSystem(lib.FileSystem):
    def __init__(
            self,
            starting_date,
            base_path,
            resource_name,
            mined_hard_delete_age,
            mined_soft_delete_age,
            snippets_delete_age):
        super().__init__(
            starting_date,
            base_path,
            mined_hard_delete_age,
            mined_soft_delete_age,
            snippets_delete_age)
        if resource_name:
            self.create_fs(resource_name)

    def create_fs(self, resource_name):
        test_data = resource.find(resource_name).decode('utf-8')
        directory_tree = test_data.splitlines()

        for path in directory_tree:
            full_path = os.path.join(self.base_path, path)
            if path[-1:] == "/" and not os.path.exists(full_path):
                os.makedirs(full_path)
            else:
                folder_path = os.path.join(self.base_path, os.path.dirname(path))
                if not os.path.exists(folder_path):
                    os.makedirs(folder_path)
                os.mknod(full_path)

    def dump_fs(self, output_file):
        with open(output_file, 'w') as f:
            addresses = list()
            for address, dirs, files in os.walk(self.base_path):
                relative_path = os.path.relpath(address, self.base_path)
                for file in files:
                    addresses.append(f"{relative_path}/{file}\n")
            f.writelines(sorted(addresses))

    def _directories_list(self, path):
        return sorted(os.listdir(path))

    def _full_remove_directory(self, full_path):
        shutil.rmtree(full_path)

    def _remove_table(self, full_path):
        os.remove(full_path)
