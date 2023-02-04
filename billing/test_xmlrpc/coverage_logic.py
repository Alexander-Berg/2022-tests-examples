from butils import logger

from balance import invoker
from balance.application import getApplication


log = logger.get_logger()


class Logic(invoker.BalanceLogicBase):

    def __init__(self):
        self._anal_info = {}
        super(Logic, self).__init__()

    def __cnv_cov(self, cov_datas):
        import coverage
        cov = coverage.Coverage()
        res = {}
        for cov_context, cov_data in cov_datas.iteritems():
            res[cov_context] = {}
            for filename in cov_data.measured_files():
                cov_anal_info = None  # self._anal_info.get(filename, None)
                try:
                    cov_anal_info = cov.analysis2(filename) if cov_anal_info is None else cov_anal_info
                    # use cov.analysis2 if need excluded lines
                except coverage.CoverageException:
                    cov_anal_info = ([], [], [], [], [])

                res[cov_context][filename] = dict(executable_lines=cov_anal_info[1],
                                                  covered_lines=cov_data.lines(filename),
                                                  notrun_lines=cov_anal_info[3],
                                                  excluded_lines=cov_anal_info[2], )

        return res

    def Collect(self, context=None, reset=False):
        cov_datas = getApplication().pool.coverage_collect(reset=reset, context=context)
        if cov_datas is None:
            return {}

        return self.__cnv_cov(cov_datas)
