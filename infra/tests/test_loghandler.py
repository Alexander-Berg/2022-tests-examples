import os
import shutil
import time
import unittest
import logging
import gzip
import tempfile

import py
import six

import pytest

from skynet_logger.loghandler import RotatingHandler

if six.PY2:
    class TemporaryDirectory(object):
        def __init__(self):
            self.directory = None

        def __enter__(self):
            self.directory = tempfile.mkdtemp()
            return self.directory

        def __exit__(self, *args):
            shutil.rmtree(self.directory)
else:
    TemporaryDirectory = tempfile.TemporaryDirectory


class NullFormatter(logging.Formatter):
    def format(self, record):
        return record.getMessage()


class TestLogHandler(unittest.TestCase):
    def test_basic_logging(self):
        with TemporaryDirectory() as testBaseDir:
            filename = os.path.join(testBaseDir, 'test_log_file')
            teststrings = []
            for i in six.moves.xrange(100):
                msg = 'test_record{0}'.format(i)
                record = logging.LogRecord(None, None, None, None, msg, (), None)
                teststrings.append(record)
            try:
                handler = RotatingHandler(filename)
                handler.setFormatter(NullFormatter())
                for record in teststrings:
                    handler.handle(record)
                handler.close()
                with open(filename, 'r') as file:
                    for fileline, record in zip(file, teststrings):
                        self.assertEqual(fileline, record.getMessage() + '\n')
            finally:
                self.assertTrue(os.path.isfile(filename))

    def test_file_rotation(self):
        with TemporaryDirectory() as testBaseDir:
            filename = 'logger'
            backupCount = 15
            genCount = backupCount + 40

            teststrings = []
            for i in six.moves.xrange(genCount, 0, -1):
                msg = 'test_record{0}'.format(i)
                record = logging.LogRecord(None, None, None, None, msg, (), None)
                teststrings.append(record)

            try:
                path = os.path.join(testBaseDir, filename)
                handler = RotatingHandler(path, maxBytes=1, backupCount=backupCount)
                resultList = set([filename])
                dfn = os.path.basename(handler._getDfn())
                resultList.add(dfn)
                for i in six.moves.xrange(1, backupCount):
                    resultList.add(dfn + '.%03d.gz' % (i, ))

                handler.setFormatter(NullFormatter())
                for line in teststrings:
                    handler.emit(line)
                    handler.doRollover()

                try:
                    handler._compressThread.join()
                except AttributeError:
                    pass
                # now test what we got
                dirslist = set(os.listdir(testBaseDir))

                for a in dirslist:
                    if a not in resultList:
                        print(a, 'not in resultlist')
                for a in resultList:
                    if a not in dirslist:
                        print(a, 'not in dirslist')
                self.assertEqual(dirslist, resultList)
                dirslist.remove(filename)  # symlink to dfn, see below

                assert open(os.path.join(testBaseDir, filename), 'r').read() == ''
                assert open(os.path.join(testBaseDir, dfn), 'r').read() == ''

                for filename in dirslist:
                    filePath = os.path.join(testBaseDir, filename)
                    self.assertTrue(os.stat(filePath))
                    try:
                        f = gzip.open(filePath, 'rt')
                        content = f.read()
                        content = f.read()
                        name, number = filename.rsplit('.', 1)
                        try:
                            number = int(number)
                            self.assertEqual(content, 'test_record{0}\n'.format(number))
                        except:
                            self.assertEqual(content, '')
                    finally:
                        f.close()

            finally:
                os.system('ls -la %s' % (testBaseDir, ))

    def test_inode_change(self):
        with TemporaryDirectory() as testBaseDir:
            filename = 'logger'
            try:
                path = os.path.join(testBaseDir, filename)

                handler = RotatingHandler(path)
                # test ino == -1
                os.remove(handler.baseFilename)
                self.assertTrue(handler._needReopen())
                handler.close()
                # test ino != current ino
                handler = RotatingHandler(path)
                os.remove(handler.baseFilename)
                f = open(handler.baseFilename, 'w')
                f.close()
                self.assertTrue(handler._needReopen())
            finally:
                os.system('ls -la %s' % (testBaseDir, ))

    def test_maxbytes(self):
        with TemporaryDirectory() as testBaseDir:
            filename = 'logfile'

            msg1 = 'a' * 50
            msg2 = 'b' * 50

            rec = lambda msg: logging.LogRecord(None, None, None, None, msg, (), None)

            # We will write here
            # - aaaa
            # - aaaa logfile.XXXX-XX-XX.YYY

            # - aaaa
            # - bbbb logfile.XXXX-XX-XX

            # - bbbb logfile
            try:
                path = os.path.join(testBaseDir, filename)

                handler = RotatingHandler(path, backupCount=4, maxBytes=100)
                assert not handler.shouldRollover(None)
                handler.emit(rec('1.' + msg1))
                assert not handler.shouldRollover(None)
                handler.emit(rec('2.' + msg1))

                # Now size should be >= 100 bytes, so we should
                # rollover during emit here
                assert handler.shouldRollover(None)
                handler.emit(rec('3.' + msg1))

                assert not handler.shouldRollover(None)
                handler.emit(rec('4.' + msg2))

                assert handler.shouldRollover(None)
                handler.emit(rec('5.' + msg2))

                try:
                    handler._compressThread.join()
                except AttributeError:
                    pass

                logfile1 = os.path.join(testBaseDir, 'logfile')
                logfile2 = logfile1 + '.' + time.strftime('%Y-%m-%d', time.localtime(time.time()))
                logfile3 = logfile2 + '.' + '001'
                logfile4 = logfile2 + '.' + '002'

                assert os.path.exists(logfile1)
                assert os.path.islink(logfile1)
                assert os.readlink(logfile1) == os.path.basename(logfile2)
                assert os.path.exists(logfile2)
                assert os.path.exists(logfile3 + '.gz')
                assert os.path.exists(logfile4 + '.gz')

                data = open(logfile2, 'r').read() + \
                    gzip.open(logfile3 + '.gz', 'rt').read() + \
                    gzip.open(logfile4 + '.gz', 'rt').read()

                assert data == '%s\n%s\n%s\n%s\n%s\n' % (
                    '5.' + msg2,
                    '3.' + msg1,
                    '4.' + msg2,
                    '1.' + msg1,
                    '2.' + msg1
                )
                print(handler.shouldRollover(None))

            finally:
                os.system('ls -la %s' % testBaseDir)
                for fn in sorted(os.listdir(testBaseDir)):
                    print(fn + ':')
                    print(repr(open(testBaseDir + '/' + fn, 'rb').read()))

    @pytest.mark.xfail
    def test_daily_rotate(self):
        with TemporaryDirectory() as testBaseDir:
            filename = 'logfile'

            msg1 = 'a' * 50
            msg2 = 'b' * 50

            rec = lambda msg: logging.LogRecord(None, None, None, None, msg, (), None)

            try:
                currentDayNo = (time.time() + time.altzone) // 86400
                midnight = (currentDayNo + 1) * 86400
                timeBeforeMidnight = int(midnight - (time.time() - time.altzone))

                path = os.path.join(testBaseDir, filename)
                handler = RotatingHandler(
                    path, backupCount=3, maxBytes=100, timeDiff=time.altzone - timeBeforeMidnight + 1
                )
                dfn1 = handler._getDfn()
                assert os.readlink(os.path.join(testBaseDir, filename)) == os.path.basename(dfn1)
                handler.emit(rec(msg1))
                handler.emit(rec(msg1))
                handler.emit(rec(msg2))
                handler.emit(rec(msg2))
                handler.emit(rec(msg2))
                handler.emit(rec(msg2))
                handler.emit(rec(msg1))
                assert os.readlink(os.path.join(testBaseDir, filename)) == os.path.basename(dfn1)
                time.sleep(2)
                dfn2 = handler._getDfn()
                handler.emit(rec(msg1))
                handler.emit(rec(msg2))

                assert os.readlink(os.path.join(testBaseDir, filename)) == os.path.basename(dfn2)

                assert dfn1 != dfn2
                assert gzip.open(dfn1 + '.001.gz', 'rt').read() == '%s\n%s\n' % (msg2, msg2)
                assert gzip.open(dfn1 + '.gz', 'rt').read() == '%s\n' % (msg1, )
                assert open(dfn2, 'r').read() == '%s\n%s\n' % (msg1, msg2)

            finally:
                os.system('ls -la %s' % testBaseDir)
                for fn in sorted(os.listdir(testBaseDir)):
                    print(fn + ':')
                    print(repr(open(testBaseDir + '/' + fn, 'rb').read()))

    def test_proper_sorting(self):
        with TemporaryDirectory() as testBaseDir:
            filename = 'logfile'

            rec = lambda msg: logging.LogRecord(None, None, None, None, msg, (), None)

            try:
                path = os.path.join(testBaseDir, filename)
                open(path + '.1970-01-01', 'w')
                open(path + '.1970-01-01.001', 'w')
                open(path + '.1970-01-01.002.gz', 'w')

                handler = RotatingHandler(
                    path, backupCount=4, maxBytes=2
                )
                handler.emit(rec('a'))
                handler.emit(rec('b'))
                assert not os.path.exists(path + '.1970-01-01.002.gz')
                handler.emit(rec('c'))
                assert not os.path.exists(path + '.1970-01-01.001')
                assert not os.path.exists(path + '.1970-01-01.001.gz')
                assert os.path.exists(path + '.1970-01-01.gz')
                handler.emit(rec('d'))
                assert not os.path.exists(path + '.1970-01-01.gz')
                handler.emit(rec('e'))

                try:
                    handler._compressThread.join()
                except AttributeError:
                    pass

                curlog = os.readlink(path)
                assert curlog.startswith(filename)
                assert curlog.count('.') == 1

                path = py.path.local(testBaseDir)

                assert gzip.open(path.join(curlog + '.003.gz').strpath, 'rt').read() == 'b\n'
                assert gzip.open(path.join(curlog + '.002.gz').strpath, 'rt').read() == 'c\n'
                assert gzip.open(path.join(curlog + '.001.gz').strpath, 'rt').read() == 'd\n'
                assert path.join(curlog).read() == 'e\n'

            finally:
                os.system('ls -la %s' % testBaseDir)
                for fn in sorted(os.listdir(testBaseDir)):
                    print(fn + ':')
                    print(repr(open(testBaseDir + '/' + fn, 'rb').read()))
