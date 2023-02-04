#include "net_test.h"

#include <yandex_io/libs/logging/logging.h>

#include <curl/curl.h>
#include <curl/easy.h>
#include <curl/multi.h>

#include <list>
#include <thread>
#include <vector>

#include <asio.hpp>

namespace {
    class EndlessDownloader {
        const std::string url_;
        std::atomic<std::uint64_t> loadedBytes_{0};
        std::thread mainThread_;

    public:
        EndlessDownloader(const std::string& url)
            : url_(url)
            , mainThread_(&EndlessDownloader::mainThreadProc, this)
        {
            //            curl_global_init(CURL_GLOBAL_ALL);
        }

        std::uint64_t getLoadedBytes() {
            return loadedBytes_;
        }

        std::size_t handleBytes(char* /*data*/, size_t n, size_t l) {
            auto rval = n * l;
            loadedBytes_ += rval;
            return rval;
        }

        static std::size_t curlCb(char* data, size_t n, size_t l, void* ptr) {
            return ((EndlessDownloader*)ptr)->handleBytes(data, n, l);
        }

        void mainThreadProc() {
            while (true) {
                CURL* curlHandle = curl_easy_init();
                curl_easy_setopt(curlHandle, CURLOPT_NOPROGRESS, 1L);
                curl_easy_setopt(curlHandle, CURLOPT_VERBOSE, 0L);
                curl_easy_setopt(curlHandle, CURLOPT_SSL_VERIFYPEER, 0L);
                curl_easy_setopt(curlHandle, CURLOPT_WRITEFUNCTION, curlCb);
                curl_easy_setopt(curlHandle, CURLOPT_WRITEDATA, this);
                curl_easy_setopt(curlHandle, CURLOPT_URL, url_.c_str());
                YIO_LOG_INFO("Perform start");
                curl_easy_perform(curlHandle);
                YIO_LOG_INFO("Perform end");
                curl_easy_cleanup(curlHandle);
            }
        }
    };

    using asio::ip::tcp;
    using asio::ip::udp;

    class UdpBroadcaster {
        std::chrono::nanoseconds sendInterval_;
        std::atomic<std::uint64_t> sentCount_{0};
        std::shared_ptr<std::string> payload_;
        udp::endpoint broadcastEndpoint_;
        asio::io_service io_;
        udp::socket socket_;
        std::thread ioThread_;
        std::unique_ptr<asio::steady_timer> nextPacketTimer_;
        std::chrono::steady_clock::time_point packetSentTime_;

        static std::chrono::nanoseconds calcIntervalFromRate(int ratePerSecond) {
            std::chrono::nanoseconds oneSecond = std::chrono::seconds(1);
            return oneSecond / ratePerSecond;
        };

        static std::string generatePayload() {
            std::vector<char> result;
            result.reserve(2000);
            while (result.size() < 1000) {
                for (int i = 0; i < 127 - 32; ++i) {
                    result.push_back(' ' + i);
                }
            };
            result.resize(1000);
            std::random_shuffle(std::begin(result), std::end(result));
            return std::string(std::begin(result), std::end(result));
        }

        static udp::socket initSocket(asio::io_service& io, std::uint16_t port) {
            udp::socket rval(io, udp::endpoint(udp::v4(), port));
            rval.set_option(udp::socket::reuse_address(true));
            rval.set_option(asio::socket_base::broadcast(true));
            return rval;
        }

        void handleSend(const std::error_code& /* error */, std::size_t /* bytes */) {
            // FIXME: what about errors?
            ++sentCount_;

            auto calcNextPacketTime = [this]() {
                auto timePassed = std::chrono::steady_clock::now() - packetSentTime_;
                if (timePassed > sendInterval_) {
                    return std::chrono::nanoseconds(0);
                }
                return sendInterval_ - timePassed;
            };

            auto waitTimeForNextSend = calcNextPacketTime();
            if (waitTimeForNextSend > std::chrono::nanoseconds(10)) {
                nextPacketTimer_ = std::make_unique<asio::steady_timer>(io_, waitTimeForNextSend);
                nextPacketTimer_->async_wait(
                    [this](const std::error_code& e) {
                        if (e != asio::error::operation_aborted) {
                            sendPacket();
                        };
                    });
            } else {
                sendPacket();
            }
        }

        void sendPacket() {
            packetSentTime_ = std::chrono::steady_clock::now();
            socket_.async_send_to(asio::buffer(*payload_), broadcastEndpoint_,
                                  [this](auto error, auto bytes) {
                                      handleSend(error, bytes);
                                  });
        }

    public:
        UdpBroadcaster(std::uint64_t rate, int port = 34567)
            : sendInterval_(calcIntervalFromRate(rate))
            , payload_(std::make_shared<std::string>(generatePayload()))
            , broadcastEndpoint_(asio::ip::address_v4::broadcast(), port)
            , socket_(initSocket(io_, port))
            , ioThread_([this]() { io_.run(); })
        {
            nextPacketTimer_ = std::make_unique<asio::steady_timer>(io_, std::chrono::seconds(1));

            nextPacketTimer_->async_wait([this](const std::error_code& /*error*/) {
                sendPacket();
            });
        }

        std::uint64_t getSentCount() {
            return sentCount_;
        }
    };
} // namespace

namespace utility {

    using LastPoints = std::list<std::tuple<std::uint64_t, std::chrono::steady_clock::time_point>>;

    std::uint64_t updateAndCalcRate(LastPoints& history, std::uint64_t value, std::chrono::steady_clock::time_point when) {
        if (history.size() == 60) {
            history.front() = {value, when};
            history.splice(std::end(history), history, std::begin(history));
        } else {
            history.emplace_back(value, when);
        }
        if (history.size() == 1) {
            return 0;
        }

        auto duration = std::get<1>(history.back()) - std::get<1>(history.front());
        if (duration <= std::chrono::seconds(0)) {
            return 0;
        }
        std::uint64_t diff = std::get<0>(history.back()) - std::get<0>(history.front());
        return diff / std::chrono::duration_cast<std::chrono::seconds>(duration).count();
    }

    void testNetwork(const std::string& urlToDownload,
                     std::uint64_t udpBroadcastsPerSecond,
                     TestNetworkCallback cb) {
        LastPoints lastLoaded;
        EndlessDownloader downloader(urlToDownload);

        if (udpBroadcastsPerSecond) {
            UdpBroadcaster flooder(udpBroadcastsPerSecond);
            LastPoints lastSent;

            while (true) {
                std::this_thread::sleep_for(std::chrono::seconds(1));
                NetTestStats stats{
                    .bytesLoaded = downloader.getLoadedBytes(),
                    .broadcastsSent = flooder.getSentCount(),
                };

                auto now = std::chrono::steady_clock::now();
                stats.loadSpeed = updateAndCalcRate(lastLoaded, stats.bytesLoaded, now);
                stats.sentRate = updateAndCalcRate(lastSent, stats.broadcastsSent, now);
                cb(stats);
            }
        } else {
            while (true) {
                std::this_thread::sleep_for(std::chrono::seconds(1));
                auto now = std::chrono::steady_clock::now();

                NetTestStats stats{
                    .bytesLoaded = downloader.getLoadedBytes(),
                    .broadcastsSent = 0,
                    .sentRate = 0,
                };

                stats.loadSpeed = updateAndCalcRate(lastLoaded, stats.bytesLoaded, now);
                cb(stats);
            }
        }
    }

} // namespace utility
