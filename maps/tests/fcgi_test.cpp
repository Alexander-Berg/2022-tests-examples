#include <maps/infra/yacare/frontends/fastcgi/proto.h>
#include <maps/infra/yacare/fd.h>
#include <maps/libs/common/include/exception.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <util/system/byteorder.h>
#include <util/system/platform.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <string.h>
#include <iostream>
#include <iterator>
#include <chrono>

namespace fcgi = yacare::fcgi::proto;

// encode number helper

size_t encode(char* buffer, uint32_t x) {
    ASSERT(x < 0x80 || x >> 31 == 0);

    if (x < 0x80) {
        buffer[0] = static_cast<char>(x);
        return 1;
    }
    buffer[0] = (x>>24) | 0x80;  // leading bit as long flag
    buffer[1] = (x>>16) & 0xFF;
    buffer[2] = (x>>8) & 0xFF;
    buffer[3] = x & 0xFF;
    return 4;
}

//
// packet classes
//

#define FCGI_KEEP_CONN  1

class BeginRequest: public fcgi::Packet {
public:
    BeginRequest(size_t id, uint8_t flags = 0) :
        fcgi::Packet(fcgi::Type::BeginRequest, id, sizeof(fcgi::Packet) + 8),
        role_(HostToInet(static_cast<uint16_t>(fcgi::Role::Responder))),
        flags_(flags), reserved_{} {}

private:
    uint16_t role_ Y_DECLARE_UNUSED;
    uint8_t flags_ Y_DECLARE_UNUSED;
    uint8_t reserved_[5] Y_DECLARE_UNUSED;
} __attribute__((packed));


typedef std::vector<std::pair<std::string, std::string>> ParamList;


size_t paramsDataLength(const ParamList& params)
{
    size_t length = 0;
    for (const auto& key_val: params) {
        length += key_val.first.size() < 0x80 ? 1 : 4;
        length += key_val.second.size() < 0x80 ? 1 : 4;
        length += key_val.first.size();
        length += key_val.second.size();
    }
    return length;
}

class ParamsPacket: public fcgi::Packet {
public:
    ParamsPacket(size_t id) :
        fcgi::Packet(fcgi::Type::Params, id, sizeof(fcgi::Packet)) {}

    ParamsPacket(size_t id, const ParamList& params):
        fcgi::Packet(fcgi::Type::Params, id,
            sizeof(fcgi::Packet) + paramsDataLength(params))
    {
        char* pos = paramsData_;
        for (const auto& key_val: params) {
            pos += encode(pos, key_val.first.size());
            pos += encode(pos, key_val.second.size());
            strcpy(pos, key_val.first.c_str());
            pos += key_val.first.size();
            strcpy(pos, key_val.second.c_str());
            pos += key_val.second.size();
        }
    }

private:
    char paramsData_[65800];
} __attribute__((packed));


class StdinPacket : public fcgi::Packet {
public:
    StdinPacket(size_t id) :
        fcgi::Packet(fcgi::Type::Stdin, id, sizeof(fcgi::Packet)) {}
};

//
// socket interface
//

typedef yacare::impl::Fd Fd;

Fd connect(const std::string& path) {
    Fd fd(socket(AF_UNIX, SOCK_STREAM, 0));
    if (fd == -1) {
        throw maps::RuntimeError() << "Cannot open socket: "
            << strerror(errno);
    }

    struct sockaddr_un addr;
    addr.sun_family = AF_UNIX;
    strcpy(addr.sun_path, path.c_str());
    int retval = connect(fd, (struct sockaddr*)&addr, sizeof(addr));
    if (retval == -1) {
        throw maps::RuntimeError() << "Cannot connect to the socket: "
            << strerror(errno);
    }

    // 60 sec timeout
    struct timeval tv;
    tv.tv_sec = 60;
    tv.tv_usec = 0;
    retval = setsockopt(fd, SOL_SOCKET, SO_RCVTIMEO, (char *)&tv,
        sizeof(struct timeval));
    if (retval == -1) {
        throw maps::RuntimeError()
            << "Cannot set timeout option of the socket: "
            << strerror(errno);
    }

    return fd;
}

std::string read(int fd) {
    std::string response;
    std::vector<char> buffer(65800, 0);
    char* pos = &buffer[0];

    while (true) {
        auto len = recv(fd, pos, &buffer[0] + buffer.size() - pos, 0);
        if (len == 0) {
            break;
        } else if (len == -1) {
            if (errno == EAGAIN || errno == EWOULDBLOCK) {
                throw maps::RuntimeError() << "timeout";
            } else if (errno == EINTR) {
                continue;
            } else {
                throw maps::RuntimeError() << strerror(errno);
            }
        }

        pos += len;
        char* p = &buffer[0];
        while (true) {
            if (p + sizeof(fcgi::Packet) > pos) {
                break;
            }
            fcgi::Packet& pkt = *reinterpret_cast<fcgi::Packet*>(p);
            if (p + pkt.totalLength() > pos) {
                break;
            }

            if (pkt.type() == fcgi::Type::Stdout) {
                if (pkt.size() > 0) {
                    std::copy(p + sizeof(fcgi::Packet),
                        p + sizeof(fcgi::Packet) + pkt.size(),
                        std::back_inserter(response));
                }
            } else if (pkt.type() == fcgi::Type::Stderr) {
            } else if (pkt.type() == fcgi::Type::EndRequest) {
                return response;
            }

            p += pkt.totalLength();
        }

        memmove(&buffer[0], p, pos - p);
        pos -= (p - &buffer[0]);
    }
    return response;
}

//
// test
//

Y_UNIT_TEST_SUITE(fcgi_test_suite) {
// Detect a race condition in yacare/fastcgi in releasing a request.
//
// A client makes a request to yacare over fascgi protocol using a specific
// request id `rid`. After it gets a response and an EndRequest packet from
// yacare, it should be possible to use the `rid` as the request id for a new
// request on the same connection.
constexpr int TEST_DURATION_SEC = 2;
constexpr int MIN_REQUESTS = 2;

Y_UNIT_TEST(fcgi_finish_race)
{
    Fd sock = connect("testapp.sock");
    EXPECT_GE(sock, 0);

    // packets for a ping request
    BeginRequest begin(1, FCGI_KEEP_CONN);
    ParamsPacket paramsPkt(1, {
        { "REQUEST_METHOD", "GET" },
        { "SCRIPT_NAME", "/" },
        { "PATH_INFO", "mtroute/ping" }
    });
    ParamsPacket paramsEnd(1);
    StdinPacket  stdinPkt(1);

    auto start = std::chrono::steady_clock::now();  // will override later
    int requests_ran = 0;
    while(true) {
        yacare::impl::ensureWrite(sock, &begin, begin.totalLength(),
            "Cannot send begin packet.");
        yacare::impl::ensureWrite(sock, &paramsPkt, paramsPkt.totalLength(),
            "Cannot send params packet.");
        yacare::impl::ensureWrite(sock, &paramsEnd, paramsEnd.totalLength(),
            "Cannot send params end packet.");
        yacare::impl::ensureWrite(sock, &stdinPkt, stdinPkt.totalLength(),
            "Cannot send stdin packet.");

        // catch timeout, which means that yacare dropped our request.
        std::string response;
        EXPECT_NO_THROW(response = read(sock));
        static const std::string HDR = "Status: 200 OK\r\n";
        EXPECT_GE(response.size(), HDR.size());
        EXPECT_EQ(response.substr(0, HDR.size()), HDR);

        requests_ran++;
        auto now = std::chrono::steady_clock::now();
        if (requests_ran <= MIN_REQUESTS) {
            // We want to run at least several requests, but
            // the first request can sometimes take several seconds to complete
            start = now;
        } else if (std::chrono::duration<double>(now - start).count()
                > TEST_DURATION_SEC) {
            break;
        }
    }
}

// /large_response handler stuck after sending about 12K of response.
// Shell be addressed later.
#if defined(_linux_)
Y_UNIT_TEST(fcgi_large_response) {
    Fd sock = connect("testapp.sock");
    EXPECT_GE(sock, 0);

    BeginRequest beginPkt(1, FCGI_KEEP_CONN);
    ParamsPacket paramsPkt(1, {
        { "REQUEST_METHOD", "GET" },
        { "SCRIPT_NAME", "/" },
        { "PATH_INFO", "large_response" }
    });
    ParamsPacket paramsEndPkt(1);
    StdinPacket stdinPkt(1);

    yacare::impl::ensureWrite(sock, &beginPkt, beginPkt.totalLength(),
        "Cannot send begin packet.");
    yacare::impl::ensureWrite(sock, &paramsPkt, paramsPkt.totalLength(),
        "Cannot send params packet.");
    yacare::impl::ensureWrite(sock, &paramsEndPkt, paramsEndPkt.totalLength(),
        "Cannot send params end packet.");
    yacare::impl::ensureWrite(sock, &stdinPkt, stdinPkt.totalLength(),
        "Cannot send stdin packet.");

    static const size_t HDR_SIZE = 70;
    static const size_t BODY_SIZE = 45 * 1024 * 1024;
    EXPECT_EQ(read(sock).size(), HDR_SIZE + BODY_SIZE);
}
#endif
}
