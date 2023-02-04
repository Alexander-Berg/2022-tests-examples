from yatest import common

import os


def make_bins_folder(bins):
    bdir = os.path.abspath('binaries_dir')
    if not os.path.exists(bdir):
        os.makedirs(bdir)
    b_paths = {'rtyserver_test': 'saas/rtyserver_test',
               'rtyserver': 'saas/rtyserver',
               'searchproxy': 'saas/searchproxy',
               'indexerproxy': 'saas/indexerproxy'
               }
    for bin_name, bin_path in b_paths.items():
        if bin_name not in bins:
            continue
        arc_bin_path = common.binary_path(os.path.join(bin_path, bin_name))
        loc_bin_path = os.path.join(bdir, bin_name)
        if not os.path.exists(loc_bin_path):
            os.symlink(arc_bin_path, loc_bin_path)
    return bdir
