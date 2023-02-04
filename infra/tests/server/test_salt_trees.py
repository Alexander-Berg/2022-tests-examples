import os

import pytest

from infra.diskmanager.lib import diskmanager
from infra.diskmanager.lib import dirutil


def test_ensure_rtc_place_tree(monkeypatch):
    expected_dirs = {
        '/db/BASE',
        '/db/bsconfig',
        '/db/bsconfig/webcache',
        '/db/www',
        '/db/www/logs',
        '/db/bin',
        '/place/db',
    }
    expected_links = {
        '/db': '/place/db',
        '/usr/local/www': '/place/db/www',
    }
    dirs = set()
    links = {}
    def _track_dir(*args, **kwargs):
        dirs.add(args[0])

    def _track_link(*args, **kwargs):
        links[args[0]] = args[1]

    monkeypatch.setattr(os.path, 'isdir', lambda *args, **kwargs: False)
    monkeypatch.setattr(dirutil, 'ensure_dir', _track_dir)
    monkeypatch.setattr(dirutil, 'ensure_link', _track_link)

    diskmanager._ensure_rtc_place_tree()
    assert dirs == expected_dirs
    assert links == expected_links


def test_ensure_rtc_place_tree_yt(monkeypatch):
    expected_dirs = {
        '/db/BASE',
        '/db/bsconfig',
        '/db/bsconfig/webcache',
        '/db/www',
        '/db/www/logs',
        '/db/bin',
        '/place/db',
        '/yt',
    }
    expected_links = {
        '/db': '/place/db',
        '/usr/local/www': '/place/db/www',
    }
    dirs = set()
    links = {}

    def _track_dir(*args, **kwargs):
        dirs.add(args[0])

    def _track_link(*args, **kwargs):
        links[args[0]] = args[1]

    monkeypatch.setattr(os.path, 'isdir', lambda *args, **kwargs: True)
    monkeypatch.setattr(dirutil, 'ensure_dir', _track_dir)
    monkeypatch.setattr(dirutil, 'ensure_link', _track_link)

    diskmanager._ensure_rtc_place_tree()
    assert dirs == expected_dirs
    assert links == expected_links


def test_ensure_rtc_ssd_tree(monkeypatch):
    expected_dirs = {
        '/ssd/webcache',
        '/ssd',
        '/ssd/porto_layers',
        '/ssd/porto_volumes',
    }
    dirs = set()
    def _track_dir(*args, **kwargs):
        dirs.add(args[0])

    monkeypatch.setattr(os.path, 'isdir', lambda *args, **kwargs: False)
    monkeypatch.setattr(dirutil, 'ensure_dir', _track_dir)
    monkeypatch.setattr(os, 'mkdir', _track_dir)

    diskmanager._ensure_rtc_ssd_tree()
    assert dirs == expected_dirs


@pytest.mark.parametrize("have_ssd,base", [(True, 'ssd'), (False, 'place')])
def test_ensure_rtc_basesearch_tree(monkeypatch, have_ssd, base):
    expected_dirs = {
        '/{}/basesearch'.format(base),
    }
    expected_links = {
        '/basesearch': '/{}/basesearch'.format(base),
    }
    dirs = set()
    links = {}

    def _track_dir(*args, **kwargs):
        dirs.add(args[0])

    def _track_link(*args, **kwargs):
        links[args[0]] = args[1]

    monkeypatch.setattr(dirutil, 'ensure_dir', _track_dir)
    monkeypatch.setattr(dirutil, 'ensure_link', _track_link)

    diskmanager._ensure_rtc_basesearch_tree(have_ssd)
    assert dirs == expected_dirs
    assert links == expected_links
