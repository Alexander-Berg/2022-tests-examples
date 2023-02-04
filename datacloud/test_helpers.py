import unittest
from datacloud.ml_utils.dolphin.prepare_cse.helpers import (
    MarkCidsMapper,
    mark_eids_reducer,
    list_suffixes,
    compact_reducer,
    make_file_name
)


class TestMarkCids(unittest.TestCase):
    def test_mark_cids_mapper(self):
        mapper = MarkCidsMapper(
            nfolds=2,
            val_sample_rate=10
        )

        self.assertEqual(
            list(mapper({'cid': 11, 'external_id': 1, 'other_field': '123'})),
            [{'cid': 11, 'external_id': 1, 'mark': '2'}]
        )
        self.assertEqual(
            list(mapper({'cid': 12, 'external_id': 2, 'other_field': '321'})),
            [{'cid': 12, 'external_id': 2, 'mark': '1'}]
        )
        self.assertEqual(
            list(mapper({'cid': 20, 'external_id': 2, 'other_field': '312'})),
            [{'cid': 20, 'external_id': 2, 'mark': 'val'}]
        )

    def test_mark_eids_reducer(self):
        self.assertEqual(
            list(mark_eids_reducer(
                key={'external_id': 1},
                recs=[
                    {'mark': '1'},
                    {'mark': '1'}
                ]
            )),
            [{'external_id': 1, 'mark': '1'}]
        )
        self.assertEqual(
            list(mark_eids_reducer(
                key={'external_id': 2},
                recs=[
                    {'mark': '2'},
                    {'mark': '1'}
                ]
            )),
            [{'external_id': 2, 'mark': 'confused'}]
        )
        self.assertEqual(
            list(mark_eids_reducer(
                key={'external_id': 3},
                recs=[
                    {'mark': '2'},
                    {'mark': '1'},
                    {'mark': 'val'}
                ]
            )),
            [{'external_id': 3, 'mark': 'confused'}]
        )
        self.assertEqual(
            list(mark_eids_reducer(
                key={'external_id': 4},
                recs=[{'mark': 'val'}]
            )),
            [{'external_id': 4, 'mark': 'val'}]
        )


def test_list_suffixes():
    class FakeCConfig:
        def __init__(self):
            self.n_folds = 4

    assert list_suffixes(FakeCConfig()) == [1, 2, 3, 4, 'val']


class TestCompactReducer(unittest.TestCase):
    def test_regular(self):
        self.assertEqual(
            list(compact_reducer(
                key={
                    'external_id': 1,
                    'retro_date': '2000-01-01',
                    'target': 1,
                    'partner': 'bank'
                },
                recs=[
                    {'id_type': 'phone_md5', 'id_value': '1'},
                    {'id_type': 'phone_md5', 'id_value': '2'},
                    {'id_type': 'email_md5', 'id_value': '3'},
                ]
            )),
            [{
                'external_id': 1,
                'phone_id_value': '1,2',
                'email_id_value': '3',
                'retro_date': '2000-01-01',
                'partner': 'bank',
                'target': 1
            }]
        )

    def test_empty_phone(self):
        self.assertEqual(
            list(compact_reducer(
                key={
                    'external_id': 1,
                    'retro_date': '2000-01-01',
                    'target': 1,
                    'partner': 'bank'
                },
                recs=[{'id_type': 'email_md5', 'id_value': '3'}]
            )),
            [{
                'external_id': 1,
                'phone_id_value': None,
                'email_id_value': '3',
                'retro_date': '2000-01-01',
                'partner': 'bank',
                'target': 1
            }]
        )

    def test_bad_id_type(self):
        with self.assertRaises(ValueError):
            list(compact_reducer(
                key={
                    'external_id': 1,
                    'retro_date': '2000-01-01',
                    'target': 1,
                    'partner': 'bank'
                },
                recs=[{'id_type': 'not_a_type', 'id_value': '3'}]
            ))


def test_make_file_name():
    file_name = make_file_name('suf')
    assert type(file_name) is str
    assert 'suf' in file_name
    assert file_name.endswith('.tsv')
