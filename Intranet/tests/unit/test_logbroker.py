import pytest

from watcher.logic.logbroker import process_message


@pytest.fixture
def message():
    class Message:
        data = (b'\x1f\x8b\x08\x00\x00\x00\x00\x00\x02\xff\xac\x96'
                b'\xc9n\xdb0\x10\x86\xdfe\xaee\n\x8a\xf2"\xf1)z'
                b'\xc8-0\x04Z\x1c9DDR\xe1\x92\xc6\x08\xf2\xee\x05'
                b'\xb5\xc4Va\xb4M\xd8\x8b1#\xd1\x1f\x7f\rg\xe1\xc3'
                b'\x1b(\t\xbc.\x8abS\x97\xd5\x8e\x80\xc1\xd7\xd0{\x03'
                b'\xbc\xd8U\xc5\xb6\xa2\x1b\xbaa\xdb\x9a\x11h\xad\xd6*'
                b'\xdc+\x8d\xe9\x1d\xdbTU]\xd0\x92U\xdb\xed\xbe\xa6\x94'
                b'\x12\x08\xaf?\xacWAY\x03\x9c\x12xRF\x02\x878H\x11\x10\x08'
                b'\xf8\xf6\x11\xb5\x00\x0eC<\xf6\xaa\x05\x02A\x1c{\x04\x0e'
                b'\x1e\xdd\x8bj\xd17\xb3\xa1Q\x1f\xd1A\xda\xb2\x8f\xda\x18'
                b'\xa1\xd1\x03\x7fHJ\t\xf8 \xba\xae\x99\xcci\xf9\xe48\xdb'
                b'\xcf\x96D\xdf:5\x8cB\x08\x0c\x8b&\x02\xca7\x01\xf5\x00'
                b'\x04l\xdb\xc6A\x98\xf6\x0c\x04~\xa2:=\x86\xb4[\xf4\xc1'
                b'\xea&\x81\x92\xe7P\x04\x94\x8dH\xaf\xb4\x95\xaaS\x8b'
                b'\xd79\xab\x1b\x89\x83pA\xa3\t\xd3\xae\xc9k\xdc\xf4\xa9'
                b'\x0e\xbd\x8dnQ\xd6\xd9hd\xa3L3I\x1f\x11\x12\x07\xa7^\x16'
                b'\xe0\xe4)s\x9a\xdc\x93\x13\xe6ck\x1f&\xa6\x88\xc1:|\x8e'
                b'\xe8\x03J8,\xc1y\x11}\x1c\xa3\xc3jV\x95%)\xf7l[\x12FJ\xba'
                b'\'\x00\x84\x92N\xf4\x1e\t%4\xb9\xc0(+\xee\xe8\xee\x8en\xee'
                b'\x8b-/(\xa7\xec;\xad\xab\xdd\xa6\xfeFKN\xe9\x1f\x96\xec'
                b'\xd9\xb2\xc4\xc4\xbe\xff\xcb\x0f\\\xa4\x92\xe0"\x1e\xe6'
                b'\xd3n\x96,xx\x83A\x84G\xe0IU:a\xe0\xd3\xf9\x86\xf30\xda&'
                b'\xec6@\xe0\t\xcf\xc0\x13aB*\x87\x12\xf8\xf8M\xef\xe4\x06'
                b'\xe2*;n\x81\xa6`\xfc\x1b\xe9:\xb92Y\x97\xdc\xcc\x04\xadS{'
                b'\x86\xc5\xd0U_`]\x15\xc6EU\xc9\xbe@\xba\xd4\xd5\x0c:Z\xdb'
                b'\xa30_@]\x17f\xa6\xaa\x8f\xba\xce\xe4\xac\xdbB^\xccWM%\x0f'
                b'\xb5nIy\xac\x9b\r-3U\xaf\xfb\xe1\x8c\x926\x1e\xc7 ~\xba~V'
                b'\xfd4\xf3<o\xb5\xe3\xbc\xf0\xad\x9b\xf9\xff`}\x8c\x82<\xd8j'
                b'\x90\xe4\xa1\x961\x94GY\x0f\xb1O\xf6\x8b\x03\x01\xdb\xcb\'<{'
                b'\xe0oi\xe5\xf5\xc5\xe00\xfe7\xf1\xc6\'GuR&\xccO\x7f\x9b\x91'
                b'\x87\xf7tWI\xb94J{\x8e\xe8\xce\xc9|?\xfc\x02\x00\x00\xff'
                b'\xff\x01\x00\x00\xff\xff]\x9e\xf1\xaf\x0e\t\x00\x00'
                )
    return Message()


def test_process_message(message):
    result = list(process_message(message))
    assert result == [
        ('services_servicemember',
         'update',
         {
             'autorequested': True,
             'created_at': '2021-06-04T15:10:02.098649+03:00',
             'custom_role': '',
             'deprived_at': None,
             'depriving_at': None,
             'description': '',
             'found_in_staff_at': None,
             'from_department_id': None,
             'granted_at': None,
             'id': 292833,
             'is_temp': False,
             'modified_at': '2021-06-04T15:10:02.098672+03:00',
             'occupancy': 0,
             'part_rate': None,
             'position': 0,
             'resource_id': None,
             'role_id': 307,
             'service_id': 2,
             'staff_id': 37253,
             'state': 'requested',
             'weight': 0,
         },
         {
             'id': 292833
         },
         )
    ]
