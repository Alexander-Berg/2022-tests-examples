#! /usr/bin/env python3
import unittest
from libs.packages import Packages

package1 = '''Package: a11y-profile-manager
Priority: optional
Section: misc
Installed-Size: 27
Maintainer: Luke Yelavich <themuso@ubuntu.com>
Architecture: amd64
Version: 0.1.10-0ubuntu3
Depends: liba11y-profile-manager-0.1-0 (>= 0.1.3), libc6 (>= 2.4), libglib2.0-0 (>= 2.26.0)
Filename: pool/main/a/a11y-profile-manager/a11y-profile-manager_0.1.10-0ubuntu3_amd64.deb
Size: 6276
MD5sum: a9d0d5ead1c417e81cadd0227f285f92
SHA1: b63e87f06fa29f33f4990d9d8563752b6c6f7910
SHA256: 863d375123f65eb2bef53fbea936379275da9e9b63dc18ba378cb3a4adcc82eb
Description: Accessibility Profile Manager - Command-line utility
Multi-Arch: foreign
Homepage: https://launchpad.net/a11y-profile-manager
Description-md5: ecbac70f8ff00c7dbf5fdc46d7819613
Bugs: https://bugs.launchpad.net/ubuntu/+filebug
Origin: Ubuntu
Supported: 5y
Task: ubuntu-live, ubuntu-gnome-desktop
'''

package2 = '''Package: account-plugin-google
Priority: optional
Section: gnome
Installed-Size: 29
Maintainer: Ubuntu Desktop Team <ubuntu-desktop@lists.ubuntu.com>
Architecture: all
Source: account-plugins
Version: 0.12+16.04.20160126-0ubuntu1
Depends: libaccount-plugin-google | ubuntu-system-settings-online-accounts, unity-asset-pool (>> 0.8.24daily12.12.05-0ubuntu1)
Filename: pool/main/a/account-plugins/account-plugin-google_0.12+16.04.20160126-0ubuntu1_all.deb
Size: 3368
MD5sum: 34f13e0dda9f3897eee5e83a07bdf6a5
SHA1: 0266c67ba88e9705c1d3d2e205bd5b045678972b
SHA256: 46a6315415046881ae8c588d09210df705898bf02d9b1d5b3c0d5ff0ace8e492
Description: GNOME Control Center account plugin for single signon
Homepage: https://launchpad.net/account-plugins
Description-md5: 33423c62c8cdfe0c6a499715b18fd300
Bugs: https://bugs.launchpad.net/ubuntu/+filebug
Origin: Ubuntu
Supported: 5y
Task: ubuntu-desktop, ubuntu-usb, edubuntu-desktop, edubuntu-usb, ubuntu-touch, ubuntukylin-desktop, ubuntu-mate-desktop
'''

class PackagesTest(unittest.TestCase):

    def testLoadNotExistFile(self):
        with self.assertRaises(Exception) as context:
            dpkgScan = Packages('./tests/Packages.gzdsfoisjdafij')
            dpkgInfo = dpkgScan.Next()
        self.assertRaises(TypeError, context.exception)

    def testLoadIfExistButISNotArchive(self):
        with self.assertRaises(Exception) as context:
            dpkgScan = Packages('./main.py')
            dpkgInfo = dpkgScan.Next()
        self.assertRaises(TypeError, context.exception)

    def testLoad1(self):
        dpkgScan = Packages('./tests/Packages.gz')
        self.assertTrue(dpkgScan.Next() == package1)

    def testLoad2(self):
        dpkgInfo = None
        dpkgScan = Packages('./tests/Packages.gz')
        for i in range(5):
            dpkgInfo = dpkgScan.Next()
        self.assertTrue(dpkgScan.Next() == package2)

if __name__ == '__main__':
    unittest.main()
