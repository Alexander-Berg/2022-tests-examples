import pytest

from idm.metrics.abc_slug_paths import count_wrong_slug_paths

pytestmark = [pytest.mark.django_db]


def test_wrong_slug_paths_count(complex_system):
    assert count_wrong_slug_paths(complex_system) == 0

    node = complex_system.nodes.get(slug='subs')
    node.slug_path = 'wrong_path'
    node.save(update_fields=['slug_path'])

    assert count_wrong_slug_paths(complex_system) == node.get_descendants(include_self=True).count()
