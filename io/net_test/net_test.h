#pragma once

#include <cstdint>
#include <functional>
#include <string>

namespace utility {
    struct NetTestStats {
        std::uint64_t bytesLoaded;
        std::uint64_t loadSpeed; // bytes per second last minute average
        std::uint64_t broadcastsSent;
        std::uint64_t sentRate; // packets per second last minute average
    };

    using TestNetworkCallback = std::function<void(const NetTestStats&)>;

    void testNetwork(const std::string& urlToDownload,
                     std::uint64_t udpBroadcastsPerSecond,
                     TestNetworkCallback /*cb*/);
} // namespace utility
