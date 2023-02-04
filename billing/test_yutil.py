import unittest

try:
    import elementtree.ElementTree as et
except:
    import xml.etree.ElementTree as et

import yutil
import os

import logging
log = logging.getLogger('test_yutil')

class TestReadConfig(unittest.TestCase):
    
    def setUp(self):
        self.saved_env_name = os.environ.get(yutil.cfg_env_var, '')
        
    def tearDown(self):
        os.environ[yutil.cfg_env_var] = self.saved_env_name 
    
    def test_success_read_config_env(self):
        cfname = 'test/test.cfg'
        os.environ[yutil.cfg_env_var] = cfname
        (name, cfg) = yutil.read_config()
        self.assert_(cfg, "empty config object")
        self.assertEqual(name, cfname, "wrong config name")
        #
        s1 = cfg.find('Setting1').text
        self.assertEqual(s1, '1')
        s1 = cfg.find('Setting2').text
        self.assertEqual(s1, 'abc')
    
    def test_success_read_config_possible(self):
        # should save it in prepare and restore in teardown, but still lazy
        del os.environ[yutil.cfg_env_var]
        (name, cfg) = yutil.read_config()
        self.assert_(cfg, "empty config object")
        self.assert_(name, "empty file name")
        #
        s1 = cfg.find('Setting1').text
        self.assertEqual(s1, '1')
        s1 = cfg.find('Setting2').text
        self.assertEqual(s1, 'abc')
    
    #def test_success_read_config_servant(self):
    # TODO: invent and implement

class TestReadConfig2(unittest.TestCase):
    def setUp(self):
        self.a_string = 'abc'
        self.a_number = 123
        self.cfg = et.fromstring('''
            <Config>
                <Test>
                    <Trim>  %s &#9;  </Trim>
                    <GoodInt>%d</GoodInt>
                    <BadInt>%dzzz</BadInt>
                </Test>
            </Config>
            ''' % (self.a_string, self.a_number, self.a_number)
        )
    
    def test_success_read_str(self):
        self.assertEquals(
            yutil.get_cfg_value(self.cfg, 'Test/GoodInt'), str(self.a_number)
        )

    def test_success_trim_str(self):
        self.assertEquals(
            yutil.get_cfg_value(self.cfg, 'Test/Trim'), str(self.a_string)
        )

    def test_success_read_int(self):
        self.assertEquals(
            yutil.get_cfg_value(self.cfg, 'Test/GoodInt', int), self.a_number
        )

    def test_fail_read_str(self):
        self.assertRaises(Exception, 
            yutil.get_cfg_value, self.cfg, 'Test/Nonexistent'
        )

    def test_fail_parse_int(self):
        self.assertRaises(Exception, 
            yutil.get_cfg_value, self.cfg, 'Test/BadInt', int
        )

class _TestDatabase(unittest.TestCase):
    def test_success_connect_to_oracle_database(self):
        cfg = et.fromstring('''
        <Config>
          <DbBackend id="default" type="oracle">
            <User>bo</User>
            <Pass>balalancing</Pass>
            <Host>test2</Host>
          </DbBackend>
        </Config>''')

        connection = yutil.process_db_config(cfg)

        self.assertEqual(connection.username, 'bo')
        self.assertEqual(connection.password, 'balalancing')
        self.assertEqual(connection.tnsentry, 'test2')

    def test_fail_connect_to_oracle_database(self):
        cfg = et.fromstring('''
        <Config>
          <DbBackend id="default" type="oracle">
            <User>bo</User>
            <Pass>blablabla</Pass>
            <Host>test2</Host>
          </DbBackend>
        </Config>''')

        self.assertRaises(Exception, yutil.process_db_config, cfg)

    def test_success_connect_to_mysql_database(self):
        cfg = et.fromstring('''
        <Config>
          <DbBackend id="default" type="mysql">
            <User>test_user</User>
            <Pass>test_pass</Pass>
            <Db>test</Db>
            <Host>localhost</Host>
          </DbBackend>
        </Config>''')

        connection = yutil.process_db_config(cfg)

    def test_fail_connect_to_mysql_database(self):
        cfg = et.fromstring('''
        <Config>
          <DbBackend id="default" type="mysql">
            <User>test_user</User>
            <Pass>blablabla</Pass>
            <Db>test</Db>
            <Host>localhost</Host>
          </DbBackend>
        </Config>''')

        self.assertRaises(Exception, yutil.process_db_config, cfg)

    def test_non_default_db_backend(self):
        cfg = et.fromstring('''
        <Config>
          <DbBackend id="aaa" type="mysql">
            <User>test_user</User>
            <Pass>test_pass</Pass>
            <Db>test</Db>
            <Host>localhost</Host>
          </DbBackend>
        </Config>
        ''')

        connection = yutil.process_db_config(cfg, 'aaa')

if __name__ == '__main__':
    unittest.main()
