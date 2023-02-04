from yabs.logconfig import get_logs_regexp
from yabs.tabtools import Grep, mr_do_map

if __name__ == "__main__":
    logs = get_logs_regexp("matrixnet/mx-source-EFHWClicked05NS-2015(09(1[56789]|[23].*)|100[123])")
    mr_do_map([Grep('r.ProductType == 2')],
              src_tables=[logs],
              dst_tables=["users/ilariia/Performance/EFHWClicked05NS_performance_20150915_20151003"])
