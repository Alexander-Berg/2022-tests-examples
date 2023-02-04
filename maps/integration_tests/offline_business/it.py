# to run:
#  ya make -trAP --test-stderr --keep-temps -v --test-param=run_with_yt --test-traceback=long
# to run and canonize:
#  ya make -trAP --test-stderr --keep-temps -v --test-param=run_with_yt --test-traceback=long -Z
#  svn commit ...

import pytest

from maps.search.libs.integration_testlib.test_sequence import YtStagesTestBase, make_cpp_yt_init  # noqa

from pack import offline_business_data_pack


@pytest.mark.usefixtures("make_cpp_yt_init")
class TestOfflineBusiness(YtStagesTestBase):
    pack = offline_business_data_pack
