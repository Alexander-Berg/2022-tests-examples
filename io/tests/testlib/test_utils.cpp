#include "test_utils.h"

#include <yandex_io/libs/base/utils.h>
#include <yandex_io/libs/logging/logging.h>

#include <util/system/byteorder.h>
#include <library/cpp/testing/common/env.h>

#include <boost/algorithm/string.hpp>

#include <cerrno>
#include <cstring>
#include <fstream>
#include <iostream>
#include <thread>

#include <unistd.h>
#include <netinet/in.h>
#include <sys/socket.h>

namespace quasar::TestUtils {

    BoundSocket::BoundSocket(int port)
    {
        socket_ = ::socket(AF_INET, SOCK_STREAM, 0);
        // Avoid already bound messages for the minute after a server has exited
        int tr = 1;
        int res = setsockopt(socket_, SOL_SOCKET, SO_REUSEADDR, &tr, sizeof(int));
        if (res != 0)
        {
            std::cerr << (std::string("setsockopt: ") + strerror(errno));
            close();
            throw std::runtime_error(std::string("setsockopt: ") + strerror(errno));
        }
        port_ = port;
        auto bindPort = [&](int port) {
            struct sockaddr_in sin;
            memset(&sin, 0, sizeof(sin));
            sin.sin_family = AF_INET;
            sin.sin_port = HostToInet<uint16_t>(port);
            sin.sin_addr.s_addr = INADDR_ANY;

            int res = ::bind(socket_,
                             reinterpret_cast<sockaddr*>(&sin),
                             sizeof(sin));
            if (res == -1 && errno != EADDRINUSE)
            {
                close();
                throw std::runtime_error(std::string("listen: bind returned: ") + strerror(errno));
            }

            return res == 0;
        };
        if (!bindPort(port_)) {
            close();
            throw std::runtime_error(
                "Cannot bind port in range " + std::to_string(port_));
        }
    }

    int BoundSocket::socket() const {
        return socket_;
    }

    int BoundSocket::port() const {
        return port_;
    }

    void BoundSocket::listen() const {
        if (::listen(socket(), SOMAXCONN) != 0)
        {
            std::cerr << (std::string("listen: ") + strerror(errno));
            throw std::runtime_error(std::string("listen: ") + strerror(errno));
        }
    }

    int BoundSocket::acceptSocket() const {
        sockaddr_in sin;
        socklen_t addrLen = sizeof(sin);
        int acceptedSock = accept(socket(), reinterpret_cast<sockaddr*>(&sin), &addrLen);
        if (acceptedSock < 0)
        {
            throw std::runtime_error(std::string("accept error: ") + strerror(errno));
        }

        return acceptedSock;
    }

    void BoundSocket::close()
    {
        Y_VERIFY(socket() != -1);
        ::close(socket());
        socket_ = -1;
    }

    BoundSocket::~BoundSocket()
    {
        if (socket_ != -1) {
            close();
        }
    }

    std::unordered_map<std::string, std::string> parseHttpFormData(const std::string& data)
    {
        std::vector<std::string> elements;
        boost::iter_split(elements, data, boost::first_finder("&"));

        std::unordered_map<std::string, std::string> result;
        for (const std::string& element : elements)
        {
            std::vector<std::string> keyValue;
            boost::iter_split(keyValue, element, boost::first_finder("="));
            if (2 == keyValue.size()) {
                result[keyValue[0]] = keyValue[1];
            }
        }

        return result;
    }

    bool doUntil(const std::function<bool()>& func, int timeoutMs)
    {
        const int tries = std::max(timeoutMs / 50, 1);

        if (func()) {
            return true;
        }

        for (int i = 0; i < tries; ++i)
        {
            std::this_thread::sleep_for(std::chrono::milliseconds(50));
            if (func()) {
                return true;
            }
        }

        return false;
    }

    void waitUntil(const std::function<bool()>& func)
    {
        Y_VERIFY(func != nullptr);
        while (!func()) {
            std::this_thread::sleep_for(std::chrono::milliseconds(50));
        }
    }

    void waitUntil(SteadyConditionVariable& condVar, const std::function<bool()>& func)
    {
        Y_VERIFY(func != nullptr);
        std::mutex randomMutex;
        waitUntil(condVar, randomMutex, func);
    }

    void waitUntil(SteadyConditionVariable& condVar, std::mutex& mutexToLock, const std::function<bool()>& func)
    {
        Y_VERIFY(func != nullptr);
        std::unique_lock<std::mutex> randomLock(mutexToLock);
        condVar.wait(randomLock, func);
    }

    void flushCallbackQueue(const std::shared_ptr<ICallbackQueue>& callbackQueue)
    {
        callbackQueue->wait();
    }

    std::chrono::system_clock::time_point getStartOfDayUTC(std::chrono::system_clock::time_point timePoint) {
        std::tm dateTime;
        time_t time = std::chrono::duration_cast<std::chrono::seconds>(timePoint.time_since_epoch()).count();
        if (!gmtime_r(&time, &dateTime)) {
            throw std::runtime_error("problem with gmtime_r");
        }
        dateTime.tm_sec = 0;
        dateTime.tm_min = 0;
        dateTime.tm_hour = 0;
        time = mktime(&dateTime);
        if (time == -1) {
            throw quasar::ErrnoException(errno, "Error when calling mktime");
        }
        // We return UTC time here, so shift it by tm_gmtoff
        return std::chrono::system_clock::time_point(std::chrono::seconds(time + dateTime.tm_gmtoff));
    }

    std::string tryGetRamDrivePath() {
        const std::string& ramDrivePath = GetRamDrivePath();
        if (ramDrivePath.empty()) {
            YIO_LOG_DEBUG("ram drive path is not defined in env, use current working dir instead");
            return GetWorkPath();
        }
        return ramDrivePath;
    }

} // namespace quasar::TestUtils
