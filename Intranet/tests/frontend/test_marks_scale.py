# coding: utf-8
from review.core.models import MarksScale
from tests import helpers


def test_get_marks_scale(client, marks_scale_builder):
    db_scales = list(MarksScale.objects.all()) + [
        marks_scale_builder(scale={'A': 1, 'B': 2}, show_absolute=True, use_colors=False, version=0),
        marks_scale_builder(scale={'A': 1, 'B': 2, 'C': 3}, show_absolute=True, use_colors=False, version=0),
        marks_scale_builder()
    ]
    scales = helpers.get_json(client, '/frontend/marks-scales/')['marks_scales']
    assert len(db_scales) == len(scales)
    assert all(
        any(
            (existing.id == received['id'] and
             existing.scale == received['scale'] and
             existing.show_absolute == received['show_absolute'] and
             existing.use_colors == received['use_colors'] and
             existing.version == received['version'])

            for received in scales
        )
        for existing in db_scales
    )


def test_get_marks_scale_by_review(client, marks_scale_builder, review_builder):
    review_scale = marks_scale_builder()
    review = review_builder(scale=review_scale)
    marks_scale_builder()

    search_params = [
        {'reviews': [review.id]},
        {'review_activity': True},
    ]
    for params in search_params:
        resp_scales = helpers.get_json(
            client,
            '/frontend/marks-scales/',
            request=params,
        )['marks_scales']
        assert len(resp_scales) == 1
        assert resp_scales[0]['id'] == review_scale.id
