#include "http.h"

#include "crash.h"

#include <balancer/kernel/memory/chunks.h>

#include <library/cpp/http/misc/httpcodes.h>

#include <util/generic/strbuf.h>
#include <util/stream/output.h>
#include <util/string/cast.h>

#include <cctype>

namespace NSrvKernel {
    namespace NFuzzUtil {
        static void CheckMethod(const TRequestLine& line) {
            const auto& method = line.Method;

            if (method == EMethod::GET
                    || method == EMethod::HEAD
                    || method == EMethod::POST
                    || method == EMethod::PUT
                    || method == EMethod::DELETE
                    || method == EMethod::PATCH
                    || method == EMethod::TRACE
                    || method == EMethod::CONNECT
                    || method == EMethod::OPTIONS
                    || method == EMethod::ACL
                    || method == EMethod::BASELINE_CONTROL
                    || method == EMethod::BIND
                    || method == EMethod::CHECKIN
                    || method == EMethod::CHECKOUT
                    || method == EMethod::COPY
                    || method == EMethod::LABEL
                    || method == EMethod::LINK
                    || method == EMethod::LOCK
                    || method == EMethod::MERGE
                    || method == EMethod::MKACTIVITY
                    || method == EMethod::MKCALENDAR
                    || method == EMethod::MKCOL
                    || method == EMethod::MKREDIRECTREF
                    || method == EMethod::MKWORKSPACE
                    || method == EMethod::MOVE
                    || method == EMethod::ORDERPATCH
                    || method == EMethod::PROPFIND
                    || method == EMethod::PROPPATCH
                    || method == EMethod::REBIND
                    || method == EMethod::REPORT
                    || method == EMethod::SEARCH
                    || method == EMethod::UNBIND
                    || method == EMethod::UNCHECKOUT
                    || method == EMethod::UNLINK
                    || method == EMethod::UNLOCK
                    || method == EMethod::UPDATE
                    || method == EMethod::UPDATEREDIRECTREF
                    || method == EMethod::VERSION_CONTROL
               ) {
                return;
            }
            Cerr << "wrong method" << Endl;
            Crash();
        }

        static void CheckPath(const TRequestLine& line) {
            for (auto ch : line.Path.AsStringBuf()) {
                if (ch == '?' || ch == ' ' || ch == '\n' || ch == '\r' || ch == '\t') {
                    Cerr << "wrong symbol in path" << Endl;
                    Crash();
                }
            }
        }

        static void CheckCgi(const TRequestLine& line) {
            bool started = false;

            for (const char ch : line.CGI.AsStringBuf()) {
                if (started) {
                    if (ch == ' ' || ch == '\n' || ch == '\r' || ch == '\t') {
                        Cerr << "wrong symbol in cgi" << Endl;
                        Crash();
                    }
                } else {
                    if (ch != '?') {
                        Cerr << "wrong symbol in cgi start" << Endl;
                        Crash();
                    }
                    started = true;
                }
            }
        }

        static void DoCheckMajorVersion(ui8 version) {
            if (version != 1) {
                Cerr << "wrong major version" << Endl;
                Crash();
            }
        }

        template<typename T>
        static void CheckMajorVersion(const T& line) {
            DoCheckMajorVersion(line.MajorVersion);
        }

        static void DoCheckMinorVersion(ui8 version) {
            if (version > 1) {
                Cerr << "wrong minor version" << Endl;
                Crash();
            }
        }

        template<typename T>
        static void CheckMinorVersion(const T& line) {
            DoCheckMinorVersion(line.MinorVersion);
        }

        void CheckRequest(const TRequest& request) {
            const auto& requestLine = request.RequestLine();
            CheckMethod(requestLine);
            CheckPath(requestLine);
            CheckCgi(requestLine);
            CheckMajorVersion(requestLine);
            CheckMinorVersion(requestLine);
        }

        static void CheckStatus(const TResponseLine& line) {
            int code = line.StatusCode;
            // TODO(elantsev) should check for valid http code, but
            // it is unclear wether it'll break something
            // like 418 - I am a teapot.
            // See also https://st.yandex-team.ru/BALANCER-55
            if (code <= 0 || code >= 600) {
                Crash();
            }
        }

        static void CheckReason(const TResponseLine& line) {
            for (auto ch : line.Reason.AsStringBuf()) {
                if (('\0' <= ch && ch <= '\10') || ('\12' <= ch && ch <= '\37') || ('\177' <= ch && ch <= '\177')) {
                    Crash();
                }
            }
        }

        void CheckResponse(const TResponse& response) {
            const auto& responseLine = response.ResponseLine();
            CheckMajorVersion(responseLine);
            CheckMinorVersion(responseLine);
            CheckStatus(responseLine);
            CheckReason(responseLine);
        }
    }
}

