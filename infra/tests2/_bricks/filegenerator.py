from py import std
from kernel.util.pytest import TestBrick


class FileGenerator(TestBrick):
    scope = 'function'

    def setUp(self, request):
        tmpdir = request.getfixturevalue('tmpdir')

        def _generateFiles(path='', bs='16k', bsCount=1, random=True, nameTemplate='file%d', count=1):
            files = []
            for i in range(count):
                file_ = tmpdir.join(path, nameTemplate % i)
                files.append(file_)

                proc = std.subprocess.Popen(
                    [
                        'dd',
                        'if=/dev/urandom' if random else 'if=/dev/zero',
                        'of=%s' % file_.strpath,
                        'bs=%s' % bs,
                        'count=%s' % bsCount
                    ], stdout=std.subprocess.PIPE, stderr=std.subprocess.STDOUT
                )
                proc.wait()
                assert proc.returncode == 0, proc.stdout.read()
            return files
        return _generateFiles
