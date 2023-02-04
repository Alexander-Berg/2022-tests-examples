#! /usr/bin/env python3
import unittest
from libs.dpkg import DPKG

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

package3 = '''Package: systemd
Priority: required
Section: admin
Installed-Size: 18776
Maintainer: Ubuntu Developers <ubuntu-devel-discuss@lists.ubuntu.com>
Original-Maintainer: Debian systemd Maintainers <pkg-systemd-maintainers@lists.alioth.debian.org>
Architecture: amd64
Version: 229-4ubuntu4
Replaces: systemd-services, udev (<< 228-5)
Provides: systemd-services
Depends: libacl1 (>= 2.2.51-8), libapparmor1 (>= 2.9.0-3+exp2), libaudit1 (>= 1:2.2.1), libblkid1 (>= 2.19.1), libcap2 (>= 1:2.10), libcryptsetup4 (>= 2:1.4.3), libgpg-error0 (>= 1.14), libkmod2 (>= 5~), libmount1 (>= 2.26.2), libpam0g (>= 0.99.7.1), libseccomp2 (>= 2.1.0), libselinux1 (>= 2.1.9), libsystemd0 (= 229-4ubuntu4), util-linux (>= 2.27.1), mount (>= 2.26), adduser, libcap2-bin
Pre-Depends: libc6 (>= 2.17), libgcrypt20 (>= 1.6.1), liblzma5 (>= 5.1.1alpha+20120614), libselinux1 (>= 1.32)
Recommends: libpam-systemd, dbus
Suggests: systemd-ui, systemd-container
Conflicts: klogd, systemd-services
Breaks: apparmor (<< 2.9.2-1), ifupdown (<< 0.8.5~), lsb-base (<< 4.1+Debian4), lvm2 (<< 2.02.104-1), systemd-shim (<< 8-2), udev (<< 228-5)
Filename: pool/main/s/systemd/systemd_229-4ubuntu4_amd64.deb
Size: 3643306
MD5sum: f059a404e3491d2bd3c008a98242befb
SHA1: 3b77f42f7f6c34bd33cf05c02beecb6ca9c81f63
SHA256: d1f0c30982125386ce2f6f734285b993c59b171029068145b46753e85cefde6c
Description: system and service manager
Multi-Arch: foreign
Homepage: http://www.freedesktop.org/wiki/Software/systemd
Description-md5: daa2c3e0044c2c2f5adc47475a3d6969
Bugs: https://bugs.launchpad.net/ubuntu/+filebug
Origin: Ubuntu
Supported: 5y
Task: minimal, ubuntu-core, ubuntu-core
'''

package4 = '''Package: systemd-sysv
Priority: required
Section: admin
Installed-Size: 90
Maintainer: Ubuntu Developers <ubuntu-devel-discuss@lists.ubuntu.com>
Original-Maintainer: Debian systemd Maintainers <pkg-systemd-maintainers@lists.alioth.debian.org>
Architecture: amd64
Source: systemd
Version: 229-4ubuntu4
Replaces: sysvinit (<< 2.88dsf-44~), sysvinit-core, upstart (<< 1.13.2-0ubuntu10~), upstart-sysv
Pre-Depends: systemd
Conflicts: file-rc, openrc, sysvinit-core, upstart (<< 1.13.2-0ubuntu10~), upstart-sysv
Filename: pool/main/s/systemd/systemd-sysv_229-4ubuntu4_amd64.deb
Size: 16448
MD5sum: 6c0889ae607e4b6ecdcde864c25a6a6b
SHA1: 621d5b458840fca47ac46d7064cfefbed80ac9b6
SHA256: f30278dea0ff1f0530fc38e44adb6e952898a09b9fa231efb43a170e7ceb848b
Description: system and service manager - SysV links
Multi-Arch: foreign
Homepage: http://www.freedesktop.org/wiki/Software/systemd
Description-md5: 9e9b94d3800e0508e985c47fef5c1937
Bugs: https://bugs.launchpad.net/ubuntu/+filebug
Origin: Ubuntu
Supported: 5y
Task: minimal
'''

package5 = '''Package: tar
Essential: yes
Priority: required
Section: utils
Installed-Size: 804
Maintainer: Ubuntu Developers <ubuntu-devel-discuss@lists.ubuntu.com>
Original-Maintainer: Bdale Garbee <bdale@gag.com>
Architecture: amd64
Version: 1.28-2.1
Replaces: cpio (<< 2.4.2-39)
Pre-Depends: libacl1 (>= 2.2.51-8), libc6 (>= 2.17), libselinux1 (>= 1.32)
Suggests: bzip2, ncompress, xz-utils, tar-scripts
Conflicts: cpio (<= 2.4.2-38)
Breaks: dpkg-dev (<< 1.14.26)
Filename: pool/main/t/tar/tar_1.28-2.1_amd64.deb
Size: 208682
MD5sum: 4c7f7e7d5ce0bf0f42031117874c715e
SHA1: 47cb2a311d6cff151c67a6ce832c8a07b584025a
SHA256: 076bdbd95a6d0a6d7f684f96c4c21cc7204bed22aa7187f3173d855f2cfa4641
Description: GNU version of the tar archiving utility
Multi-Arch: foreign
Description-md5: 48033bf96442788d1f697785773ad9bb
Bugs: https://bugs.launchpad.net/ubuntu/+filebug
Origin: Ubuntu
Supported: 5y
Task: minimal
'''

package6 = '''Package: tar
Essential: yes
Priority: required
Section: utils
Installed-Size: 804
Maintainer: Ubuntu Developers <ubuntu-devel-discuss@lists.ubuntu.com>
Original-Maintainer: Bdale Garbee <bdale@gag.com>
Architecture: amd64
Version: 1.28-2.3
Replaces: cpio (<< 2.4.2-39)
Pre-Depends: libacl1 (>= 2.2.51-8), libc6 (>= 2.17), libselinux1 (>= 1.32)
Suggests: bzip2, ncompress, xz-utils, tar-scripts
Conflicts: cpio (<= 2.4.2-38)
Breaks: dpkg-dev (<< 1.14.26)
Filename: pool/main/t/tar/tar_1.28-2.3_amd64.deb
Size: 208682
MD5sum: 4c7f7e7d5ce0bf0f42031117874c715e
SHA1: 47cb2a311d6cff151c67a6ce832c8a07b584025a
SHA256: 076bdbd95a6d0a6d7f684f96c4c21cc7204bed22aa7187f3173d855f2cfa4641
Description: GNU version of the tar archiving utility
Multi-Arch: foreign
Description-md5: 48033bf96442788d1f697785773ad9bb
Bugs: https://bugs.launchpad.net/ubuntu/+filebug
Origin: Ubuntu
Supported: 5y
Task: minimal
'''

class PackagesTest(unittest.TestCase):
    dpkg = DPKG()

    def testCheckPackageWithParent(self):
        self.dpkg.add(package2)
        self.assertIn('account-plugin-google', self.dpkg.list())
        self.assertIn('account-plugin-google', self.dpkg.getName('account-plugin-google'))
        self.assertIn('0.12+16.04.20160126-0ubuntu1', self.dpkg.listVersions('account-plugin-google'))
        self.assertTrue(self.dpkg.getMD5Sum('account-plugin-google', '0.12+16.04.20160126-0ubuntu1') == '34f13e0dda9f3897eee5e83a07bdf6a5')
        self.assertTrue(self.dpkg.getFileName('account-plugin-google', '0.12+16.04.20160126-0ubuntu1') == 'pool/main/a/account-plugins/account-plugin-google_0.12+16.04.20160126-0ubuntu1_all.deb')
        self.assertIn('unity-asset-pool', self.dpkg.listDepends('account-plugin-google', '0.12+16.04.20160126-0ubuntu1'))
        self.assertIn('ubuntu-system-settings-online-accounts', self.dpkg.listDepends('account-plugin-google', '0.12+16.04.20160126-0ubuntu1'))
        self.assertTrue(self.dpkg.listChanges('account-plugin-google', '0.12+16.04.20160126-0ubuntu1') == set())
        self.assertTrue(self.dpkg.getParentByVersion('account-plugin-google', '0.12+16.04.20160126-0ubuntu1') == 'account-plugins')

    def testCheckParent(self):
        self.dpkg.add(package2)
        self.assertIn('account-plugins', self.dpkg.list())
        self.assertIn('account-plugins', self.dpkg.getName('account-plugins'))
        self.assertIn('0.12+16.04.20160126-0ubuntu1', self.dpkg.listVersions('account-plugins'))
        self.assertTrue(self.dpkg.getParentByVersion('account-plugins', '0.12+16.04.20160126-0ubuntu1') == None)
        self.assertTrue(self.dpkg.listChanges('account-plugin', '0.12+16.04.20160126-0ubuntu1') == '')

    def testAddPackagesWithDependsAsChanges(self):
        self.dpkg.add(package3)
        self.dpkg.add(package4)
        self.assertIn('systemd', self.dpkg.list())
        self.assertIn('229-4ubuntu4', self.dpkg.listVersions('systemd'))
        self.assertIn('systemd-sysv', self.dpkg.list())
        self.assertIn('229-4ubuntu4', self.dpkg.listVersions('systemd-sysv'))
        self.assertTrue(self.dpkg.listChanges('systemd-sysv', '229-4ubuntu4') == set())
        self.assertIn('systemd-sysv', self.dpkg.listChanges('systemd', '229-4ubuntu4'))
        self.assertNotIn('systemd', self.dpkg.listChanges('systemd', '229-4ubuntu4'))
        self.assertTrue(self.dpkg.getMD5Sum('systemd', '229-4ubuntu4') == 'f059a404e3491d2bd3c008a98242befb')
        self.assertTrue(self.dpkg.getMD5Sum('systemd-sysv', '229-4ubuntu4') == '6c0889ae607e4b6ecdcde864c25a6a6b')
        self.assertIn('systemd', self.dpkg.listDepends('systemd-sysv', '229-4ubuntu4'))
        self.assertTrue(self.dpkg.getParentByVersion('systemd-sysv', '229-4ubuntu4') == 'systemd')

    def testAddPackageAsParentWithoutChilds(self):
        self.dpkg.add(package5)
        self.assertIn('tar', self.dpkg.list())
        self.assertIn('1.28-2.1', self.dpkg.listVersions('tar'))
        self.assertTrue(self.dpkg.listChanges('tar', '1.28-2.1') == set())
        self.assertTrue(self.dpkg.getMD5Sum('tar', '1.28-2.1') == '4c7f7e7d5ce0bf0f42031117874c715e')
        self.assertTrue(self.dpkg.getParentByVersion('tar', '1.28-2.1') == None)

    def testAddPackageAsParentWithoutChildsAnotherVersion(self):
        self.dpkg.add(package6)
        self.assertIn('tar', self.dpkg.list())
        self.assertIn('1.28-2.3', self.dpkg.listVersions('tar'))
        self.assertTrue(self.dpkg.listChanges('tar', '1.28-2.3') == set())
        self.assertTrue(self.dpkg.getFileName('tar', '1.28-2.3') == 'pool/main/t/tar/tar_1.28-2.3_amd64.deb')

if __name__ == '__main__':
    unittest.main()
