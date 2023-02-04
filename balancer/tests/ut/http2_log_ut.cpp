#include "http2_log_ut.h"

#include <contrib/libs/re2/re2/re2.h>
#include <balancer/kernel/http2/server/utils/http2_log.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/logger/stream.h>

Y_UNIT_TEST_SUITE(HTTP2LogLibTest) {
    using namespace NSrvKernel::NHTTP2;

    class TTestLogLabel : public TLogLabel {
    public:
        TTestLogLabel(TString logName)
            : TLogLabel("[" + logName + "] ", " [/" + logName + "]\t")
        {}
    };


    struct TLogTest {
        TTestLogLabel NullLogLabel{"NullLog"};
        TLogger NullLogger{nullptr, NullLogLabel, Executor};

        TStringStream ErrorStream;
        TLog ErrorLog{THolder(new TStreamLogBackend(&ErrorStream))};
        TTestLogLabel ErrorLogLabel{"ErrorLog"};
        TLogger ErrorLogger{PrepareErrorLog(ErrorLog), ErrorLogLabel, Executor};

        TStringStream InfoStream;
        TLog InfoLog{THolder(new TStreamLogBackend(&InfoStream))};
        TTestLogLabel MainLogLabel{"MainLog"};
        TLog MainLog{PrepareBackend(InfoLog, ErrorLog)};
        TLogger MainLogger{&MainLog, MainLogLabel, Executor};

        TContExecutor Executor{30000};

        TVector<TLogger*> Loggers{&MainLogger, &ErrorLogger, &NullLogger};

        std::exception_ptr Exception;

        TLogTest()
        {
            MainLog.SetDefaultPriority(TLOG_DEBUG);
        }

        static TLog* PrepareErrorLog(TLog& errorLog) {
            errorLog.SetDefaultPriority(TLOG_ERR);
            return &errorLog;
        }

        static THolder<TLogBackend> PrepareBackend(TLog& infoLog, TLog& errorLog) {
            infoLog.SetDefaultPriority(TLOG_INFO);
            errorLog.SetDefaultPriority(TLOG_ERR);
            return MakeHolder<TBilogBackend>(&infoLog, &errorLog);
        }
    };


    static TString PreprocessString(TString logs) {
        static const re2::RE2 ptrSubst("ptr=0x[a-fA-F0-9]+");
        static const re2::RE2 tstampSubst("\\] ....-..-..T..:..:..[.]......(?:[+-]...)?. \\[");
        re2::RE2::GlobalReplace(&logs, ptrSubst, "ptr=0xZZZZZZZZZZZZZZZZ");
        re2::RE2::GlobalReplace(&logs, tstampSubst, "] ZZZZ-ZZ-ZZTZZ:ZZ:ZZ.ZZZZZZzZZZZ [");
        return logs;
    }

    using TTestPtr = std::pair<TLogTest*, TLogger*>;

    class TSemaphore {
    public:
        explicit TSemaphore(ui64 token = 0)
            : Token_(token)
        {}

        [[nodiscard]]
        int WaitI(TInstant timeout, TCont& cont) {
            auto ret = EWAKEDUP;
            while (!Token_) {
                ret = CV_.wait_until(cont.Executor(), timeout);
            }
            Token_ -= 1;
            return ret;
        }

        [[nodiscard]]
        int Wait(TCont& cont) {
            return WaitI(TInstant::Max(), cont);
        }

        void Signal() {
            Token_ += 1;
            CV_.notify();
        }

    private:
        ui64 Token_ = 0;
        NSrvKernel::TCoroSingleCondVar CV_;
    };

    struct TTestData {
        TLogTest* const Test;
        TLogger* const Logger;
        TCont* const Main;

        TSemaphore SubSem;
        TSemaphore MainSem;

        std::exception_ptr Exception;

        TTestData(TLogTest* test, TLogger* logger, TCont* main)
            : Test(test)
            , Logger(logger)
            , Main(main)
        {}
    };

    static void DoLogBlock(TCont* cont, void* testPtr) noexcept {
        TTestData& test = *(TTestData*)testPtr;

        try {
            UNIT_ASSERT_VALUES_EQUAL(test.SubSem.Wait(*cont), EWAKEDUP);

            Y_HTTP2_LOG_BLOCK_E((*test.Logger), TLOG_INFO, "info_block_e");
            Y_HTTP2_LOG_BLOCK_E((*test.Logger), TLOG_DEBUG, "debug_block_e");

            test.MainSem.Signal();
            UNIT_ASSERT_VALUES_EQUAL(test.SubSem.Wait(*cont), EWAKEDUP);

            Y_HTTP2_LOG_BLOCK_E((*test.Logger), TLOG_ERR, "error_block_e");
            Y_HTTP2_BLOCK_E((*test.Logger), "block_e");

            test.MainSem.Signal();
        } catch (...) {
            test.Exception = std::current_exception();
        }
    }

    static void DoMainBlock(TCont* cont, void* testPtr) noexcept {
        ui32 state1 = 1;
        TString state3 = "xxx";
        TString* state2 = &state3;
        NUT::EBadEnum badEnum = NUT::EBadEnum(666);

        TLogTest& test = *(TLogTest*)testPtr;

        try {
            ui32 cnt = 1;
            for (auto* loggerPtr : test.Loggers) {
                TStringBuilder name;
                name << "sub#" << cnt;
                cnt += 1;

                TTestData subData{&test, loggerPtr, cont};
                test.Executor.Create(DoLogBlock, &subData, name.c_str());

                Y_HTTP2_LOG_BLOCK((*loggerPtr), TLOG_INFO, "info_block", state1, state2);
                Y_HTTP2_BLOCK((*loggerPtr), "debug_block", state1, state2);

                Y_HTTP2_LOG_EVENT((*loggerPtr), TLOG_INFO, "info_event", state1, state2, badEnum);

                subData.SubSem.Signal();
                UNIT_ASSERT_VALUES_EQUAL(subData.MainSem.Wait(*cont), EWAKEDUP);

                Y_HTTP2_LOG_BLOCK((*loggerPtr), TLOG_ERR, "error_block", state1, state2, badEnum);

                subData.SubSem.Signal();
                UNIT_ASSERT_VALUES_EQUAL(subData.MainSem.Wait(*cont), EWAKEDUP);

                if (subData.Exception) {
                    std::rethrow_exception(subData.Exception);
                }

                Y_HTTP2_BLOCK((*loggerPtr), "block", state1, state2);
            }
        } catch (...) {
            test.Exception = std::current_exception();
        }
    }


    Y_UNIT_TEST(TestBlockLogging) {
        TLogTest test;

        test.Executor.Execute(DoMainBlock, &test);

        if (test.Exception) {
            std::rethrow_exception(test.Exception);
        }

        UNIT_ASSERT_VALUES_EQUAL(
            PreprocessString(test.InfoStream.Str()),
            "[MainLog] ZZZZ-ZZ-ZZTZZ:ZZ:ZZ.ZZZZZZzZZZZ [/MainLog]\tcont(name=sys_main,ptr=0xZZZZZZZZZZZZZZZZ)\t.info_block\t{state1=1, state2=xxx} \n"
            "[MainLog] ZZZZ-ZZ-ZZTZZ:ZZ:ZZ.ZZZZZZzZZZZ [/MainLog]\tcont(name=sys_main,ptr=0xZZZZZZZZZZZZZZZZ)\t..[Event] info_event\t{state1=1, state2=xxx, badEnum=FAILED_PRINT((yexception) Undefined value 666 in NSrvKernel::NHTTP2::NUT::EBadEnum. )} \n"
            "[MainLog] ZZZZ-ZZ-ZZTZZ:ZZ:ZZ.ZZZZZZzZZZZ [/MainLog]\tcont(name=sub#1,ptr=0xZZZZZZZZZZZZZZZZ)\t.info_block_e\t\n"
            "[MainLog] ZZZZ-ZZ-ZZTZZ:ZZ:ZZ.ZZZZZZzZZZZ [/MainLog]\tcont(name=sys_main,ptr=0xZZZZZZZZZZZZZZZZ)\t..error_block\t{state1=1, state2=xxx, badEnum=FAILED_PRINT((yexception) Undefined value 666 in NSrvKernel::NHTTP2::NUT::EBadEnum. )} \n"
            "[MainLog] ZZZZ-ZZ-ZZTZZ:ZZ:ZZ.ZZZZZZzZZZZ [/MainLog]\tcont(name=sub#1,ptr=0xZZZZZZZZZZZZZZZZ)\t..error_block_e\t\n"
            "[MainLog] ZZZZ-ZZ-ZZTZZ:ZZ:ZZ.ZZZZZZzZZZZ [/MainLog]\tcont(name=sub#1,ptr=0xZZZZZZZZZZZZZZZZ)\t..error_block_e [Done]\t\n"
            "[MainLog] ZZZZ-ZZ-ZZTZZ:ZZ:ZZ.ZZZZZZzZZZZ [/MainLog]\tcont(name=sub#1,ptr=0xZZZZZZZZZZZZZZZZ)\t.info_block_e [Done]\t\n"
            "[MainLog] ZZZZ-ZZ-ZZTZZ:ZZ:ZZ.ZZZZZZzZZZZ [/MainLog]\tcont(name=sys_main,ptr=0xZZZZZZZZZZZZZZZZ)\t..error_block [Done]\t\n"
            "[MainLog] ZZZZ-ZZ-ZZTZZ:ZZ:ZZ.ZZZZZZzZZZZ [/MainLog]\tcont(name=sys_main,ptr=0xZZZZZZZZZZZZZZZZ)\t.info_block [Done]\t\n"
        );

        UNIT_ASSERT_VALUES_EQUAL(
            PreprocessString(test.ErrorStream.Str()),
            "[MainLog] ZZZZ-ZZ-ZZTZZ:ZZ:ZZ.ZZZZZZzZZZZ [/MainLog]\tcont(name=sys_main,ptr=0xZZZZZZZZZZZZZZZZ)\t..error_block\t{state1=1, state2=xxx, badEnum=FAILED_PRINT((yexception) Undefined value 666 in NSrvKernel::NHTTP2::NUT::EBadEnum. )} \n"
            "[MainLog] ZZZZ-ZZ-ZZTZZ:ZZ:ZZ.ZZZZZZzZZZZ [/MainLog]\tcont(name=sub#1,ptr=0xZZZZZZZZZZZZZZZZ)\t..error_block_e\t\n"
            "[MainLog] ZZZZ-ZZ-ZZTZZ:ZZ:ZZ.ZZZZZZzZZZZ [/MainLog]\tcont(name=sub#1,ptr=0xZZZZZZZZZZZZZZZZ)\t..error_block_e [Done]\t\n"
            "[MainLog] ZZZZ-ZZ-ZZTZZ:ZZ:ZZ.ZZZZZZzZZZZ [/MainLog]\tcont(name=sys_main,ptr=0xZZZZZZZZZZZZZZZZ)\t..error_block [Done]\t\n"
            "[ErrorLog] ZZZZ-ZZ-ZZTZZ:ZZ:ZZ.ZZZZZZzZZZZ [/ErrorLog]\tcont(name=sys_main,ptr=0xZZZZZZZZZZZZZZZZ)\t.error_block\t{state1=1, state2=xxx, badEnum=FAILED_PRINT((yexception) Undefined value 666 in NSrvKernel::NHTTP2::NUT::EBadEnum. )} \n"
            "[ErrorLog] ZZZZ-ZZ-ZZTZZ:ZZ:ZZ.ZZZZZZzZZZZ [/ErrorLog]\tcont(name=sub#2,ptr=0xZZZZZZZZZZZZZZZZ)\t.error_block_e\t\n"
            "[ErrorLog] ZZZZ-ZZ-ZZTZZ:ZZ:ZZ.ZZZZZZzZZZZ [/ErrorLog]\tcont(name=sub#2,ptr=0xZZZZZZZZZZZZZZZZ)\t.error_block_e [Done]\t\n"
            "[ErrorLog] ZZZZ-ZZ-ZZTZZ:ZZ:ZZ.ZZZZZZzZZZZ [/ErrorLog]\tcont(name=sys_main,ptr=0xZZZZZZZZZZZZZZZZ)\t.error_block [Done]\t\n"
        );
    }

    class TTestException : public yexception {
        const char* what() const noexcept override {
            return "WAT?";
        }
    };

    static void DoSmth(TLogger& logger) {
        Y_HTTP2_LOG_FUNC_E(logger, TLOG_INFO);
    }

    struct TDoSmth {
        TLogger& Logger;

        ~TDoSmth() {
            DoSmth(Logger);
        }
    };

    Y_UNIT_TEST(TestBlockExceptionLogging) {
        TLogTest test;
        try {
            try {
                Y_HTTP2_LOG_BLOCK_E(test.MainLogger, TLOG_INFO, "inside");
                TDoSmth smth{test.MainLogger};
                ythrow TTestException();
            } catch (...) {
                Y_HTTP2_CATCH_ALL_E(test.MainLogger);
                DoSmth(test.MainLogger);
                throw;
            }
        } catch (const yexception& e) {
            Y_HTTP2_CATCH_E(test.MainLogger, e);
        }

        UNIT_ASSERT_VALUES_EQUAL(
            PreprocessString(test.InfoStream.Str()),
            "[MainLog] ZZZZ-ZZ-ZZTZZ:ZZ:ZZ.ZZZZZZzZZZZ [/MainLog]\tcont(nullptr)\t.inside\t\n"
            "[MainLog] ZZZZ-ZZ-ZZTZZ:ZZ:ZZ.ZZZZZZzZZZZ [/MainLog]\tcont(nullptr)\t..[UNWIND]NTestSuiteHTTP2LogLibTest::DoSmth\t\n"
            "[MainLog] ZZZZ-ZZ-ZZTZZ:ZZ:ZZ.ZZZZZZzZZZZ [/MainLog]\tcont(nullptr)\t..[UNWIND]NTestSuiteHTTP2LogLibTest::DoSmth [Done]\t\n"
            "[MainLog] ZZZZ-ZZ-ZZTZZ:ZZ:ZZ.ZZZZZZzZZZZ [/MainLog]\tcont(nullptr)\t.[UNWIND]inside [Exception]\t\n"
            "[MainLog] ZZZZ-ZZ-ZZTZZ:ZZ:ZZ.ZZZZZZzZZZZ [/MainLog]\tcont(nullptr)\t.catch ...\t(NTestSuiteHTTP2LogLibTest::TTestException) WAT? \n"
            "[MainLog] ZZZZ-ZZ-ZZTZZ:ZZ:ZZ.ZZZZZZzZZZZ [/MainLog]\tcont(nullptr)\t..NTestSuiteHTTP2LogLibTest::DoSmth\t\n"
            "[MainLog] ZZZZ-ZZ-ZZTZZ:ZZ:ZZ.ZZZZZZzZZZZ [/MainLog]\tcont(nullptr)\t..NTestSuiteHTTP2LogLibTest::DoSmth [Done]\t\n"
            "[MainLog] ZZZZ-ZZ-ZZTZZ:ZZ:ZZ.ZZZZZZzZZZZ [/MainLog]\tcont(nullptr)\t.[UNWIND]catch ... [Exception]\t\n"
            "[MainLog] ZZZZ-ZZ-ZZTZZ:ZZ:ZZ.ZZZZZZzZZZZ [/MainLog]\tcont(nullptr)\t.catch NTestSuiteHTTP2LogLibTest::TTestException\tWAT? \n"
            "[MainLog] ZZZZ-ZZ-ZZTZZ:ZZ:ZZ.ZZZZZZzZZZZ [/MainLog]\tcont(nullptr)\t.catch [Done]\t\n"
        );
    }
};
