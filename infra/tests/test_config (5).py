#! /usr/bin/env python3
import unittest
from libs.config import Config

class ConfigTest(unittest.TestCase):
    configFile = './config.yaml.example'

    def testLaod(self):
        self.assertIsNotNone(Config.load(self.configFile))

    def testNotExistFile(self):
        with self.assertRaises(Exception) as context:
            Config.load('{}sdkfjsladfl4354sdjflkasdfjl'.format(self.configFile))
        self.assertRaises(TypeError, context.exception)

    def testVariables(self):
        self.assertIn('tmp-dir', Config.load(self.configFile))
        self.assertIn('upstream-url', Config.load(self.configFile))
        self.assertIn('upstream-release', Config.load(self.configFile))
        self.assertIn('mirror-release', Config.load(self.configFile))
        self.assertIn('sign-email', Config.load(self.configFile))
        self.assertIn('upload-to', Config.load(self.configFile))
        self.assertIn('download_packages', Config.load(self.configFile))

if __name__ == '__main__':
    unittest.main()
