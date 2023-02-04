#pragma once

#include <yandex_io/libs/threading/i_callback_queue.h>
#include <yandex_io/libs/threading/steady_condition_variable.h>

#include <atomic>
#include <chrono>
#include <future>
#include <map>
#include <sstream>
#include <string>
#include <unordered_map>
#include <vector>

#include <zlib.h>

namespace quasar::TestUtils {

    struct BoundSocket {
        explicit BoundSocket(int port);

        int socket() const;
        int port() const;
        void listen() const;
        int acceptSocket() const;
        void close();
        ~BoundSocket();

    private:
        int socket_;
        int port_;
    };

    std::unordered_map<std::string, std::string> parseHttpFormData(const std::string& data);

    bool doUntil(const std::function<bool()>& func, int timeoutMs);
    void waitUntil(const std::function<bool()>& func);
    void waitUntil(SteadyConditionVariable& condVar, const std::function<bool()>& func);
    void waitUntil(SteadyConditionVariable& condVar, std::mutex& mutexToLock, const std::function<bool()>& func);
    void flushCallbackQueue(const std::shared_ptr<ICallbackQueue>& callbackQueue);
    std::string tryGetRamDrivePath();

    template <typename... Args>
    void flushCallbackQueue(Args... args)
    {
        std::vector<std::shared_ptr<ICallbackQueue>> cqs{std::forward<decltype(args)>(std::move(args))...};
        bool repeat = false;
        do {
            for (const auto& cq : cqs) {
                cq->wait();
            }
            repeat = false;
            for (const auto& cq : cqs) {
                repeat = repeat || (cq->size() > 0);
            }
        } while (repeat);
    }

    template <typename T>
    T futureTimedGet(std::future<T> f, int milliseconds = 10000)
    {
        const auto status = f.wait_for(std::chrono::milliseconds(milliseconds));
        if (status != std::future_status::ready) {
            throw std::runtime_error("Future not ready in " + std::to_string(milliseconds) + " ms");
        }

        return f.get();
    }

    inline std::string decompress_string(const std::string& str) {
        /*
         * This is a copy of compress_string function from libs/metrica/base/utils.h
         * with change deflate -> inflate functions from zlib
         */

        z_stream zs;
        memset(&zs, 0, sizeof(zs));
        constexpr int ZLIB_GZIP_HEADER_OFFSET = 16;
        if (inflateInit2(&zs, 9 + ZLIB_GZIP_HEADER_OFFSET)) {
            throw(std::runtime_error("inflateInit2 failed while compressing."));
        }

        zs.next_in = (Bytef*)str.data();
        zs.avail_in = static_cast<uInt>(str.size());

        int ret;
        char outbuffer[204800];
        std::string outstring;

        do {
            zs.next_out = reinterpret_cast<Bytef*>(outbuffer);
            zs.avail_out = sizeof(outbuffer);

            ret = inflate(&zs, Z_FINISH);

            if (outstring.size() < zs.total_out) {
                outstring.append(outbuffer, zs.total_out - outstring.size());
            }
        } while (ret == Z_OK);

        if (ret != Z_STREAM_END) {
            std::ostringstream oss;
            oss << "Exception during zlib decompression: (" << ret << ") " << zs.msg;
            throw(std::runtime_error(oss.str()));
        }
        inflateEnd(&zs);
        return outstring;
    }

    std::chrono::system_clock::time_point getStartOfDayUTC(std::chrono::system_clock::time_point timePoint);

    template <class T>
    class EventChecker {
    public:
        explicit EventChecker(std::vector<T> events)
            : events_(events)
            , currentEventIdx_(0)
        {
        }

        [[nodiscard]] bool addEvent(T t) {
            bool res = false;
            {
                std::lock_guard<std::mutex> guard(mutex_);
                res = (t == events_[currentEventIdx_]);
                ++currentEventIdx_;
            }
            CV_.notify_all();
            return res;
        }

        void waitForEvents(size_t eventsPassed) const {
            std::unique_lock<std::mutex> lock(mutex_);
            CV_.wait(lock, [this, &eventsPassed]() {
                return currentEventIdx_ >= eventsPassed;
            });
        }

        void waitAllEvents() const {
            std::unique_lock<std::mutex> lock(mutex_);
            CV_.wait(lock, [this]() {
                return currentEventIdx_ == events_.size();
            });
        }

    private:
        std::vector<T> events_;
        size_t currentEventIdx_;

        mutable std::mutex mutex_;
        mutable SteadyConditionVariable CV_;
    };

} // namespace quasar::TestUtils
