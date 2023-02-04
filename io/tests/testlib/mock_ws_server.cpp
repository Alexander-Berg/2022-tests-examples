#include "mock_ws_server.h"

#include <yandex_io/libs/logging/logging.h>

#include <library/cpp/testing/common/env.h>

using namespace quasar::TestUtils;

void MockWSServer::startServer() {
    server_.start_accept();
    server_.run();
}

void MockWSServer::on_message(websocketpp::connection_hdl hdl, message_ptr msg) {
    std::unique_lock<std::mutex> lock(mutex_);
    connections_.insert(hdl);
    lock.unlock();
    bool text = msg->get_opcode() == websocketpp::frame::opcode::text;
    if (text && onMessage) {
        onMessage(msg->get_payload());
    }
    if (!text && onBinaryMessage) {
        onBinaryMessage(msg->get_payload());
    }
}

void MockWSServer::on_open(websocketpp::connection_hdl hdl) {
    std::unique_lock<std::mutex> lock(mutex_);
    connections_.insert(hdl);
    connection_opened_cnt++;
    connectedWakeupVar_.notify_one();
    lock.unlock();
    if (onOpen) {
        onOpen(server_.get_con_from_hdl(hdl));
    }
}

void MockWSServer::on_close(websocketpp::connection_hdl hdl) {
    std::unique_lock<std::mutex> lock(mutex_);
    connections_.erase(hdl);
    connection_closed_cnt++;
    connectedWakeupVar_.notify_one();
}

MockWSServer::context_ptr MockWSServer::on_tls_init(websocketpp::connection_hdl /* hdl */) {
    namespace asio = websocketpp::lib::asio;
    context_ptr ctx = websocketpp::lib::make_shared<asio::ssl::context>(asio::ssl::context::tlsv12);
    try {
        ctx->set_options(asio::ssl::context::default_workarounds |
                         asio::ssl::context::no_sslv2 |
                         asio::ssl::context::no_sslv3 |
                         asio::ssl::context::single_dh_use);
        ctx->set_password_callback(std::bind([&]() { return "test"; }));
        ctx->use_certificate_chain_file(ArcadiaSourceRoot() + "/yandex_io/misc/websocket_server_test.pem");
        ctx->use_private_key_file(ArcadiaSourceRoot() + "/yandex_io/misc/websocket_server_test.pem", asio::ssl::context::pem);
        std::string ciphers;
        ciphers = "ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-ECDSA-AES256-GCM-SHA384:DHE-RSA-AES128-GCM-SHA256:DHE-DSS-AES128-GCM-SHA256:kEDH+AESGCM:ECDHE-RSA-AES128-SHA256:ECDHE-ECDSA-AES128-SHA256:ECDHE-RSA-AES128-SHA:ECDHE-ECDSA-AES128-SHA:ECDHE-RSA-AES256-SHA384:ECDHE-ECDSA-AES256-SHA384:ECDHE-RSA-AES256-SHA:ECDHE-ECDSA-AES256-SHA:DHE-RSA-AES128-SHA256:DHE-RSA-AES128-SHA:DHE-DSS-AES128-SHA256:DHE-RSA-AES256-SHA256:DHE-DSS-AES256-SHA:DHE-RSA-AES256-SHA:!aNULL:!eNULL:!EXPORT:!DES:!RC4:!3DES:!MD5:!PSK";

        if (SSL_CTX_set_cipher_list(ctx->native_handle(), ciphers.c_str()) != 1) {
            YIO_LOG_WARN("Error setting cipher list");
        }
    } catch (const std::exception& e) {
        YIO_LOG_WARN("Exception: " << e.what());
    }
    return ctx;
}

MockWSServer::~MockWSServer() {
    std::unique_lock<std::mutex> lock(mutex_);
    server_.stop();
    lock.unlock();
    server_thread_.join();
}
MockWSServer::MockWSServer(int port) {
    using std::bind;
    using std::placeholders::_1;
    using std::placeholders::_2;
    std::unique_lock<std::mutex> lock(mutex_);
    connection_opened_cnt = 0;
    connection_closed_cnt = 0;
    server_.init_asio();
    server_.set_reuse_addr(true);
    server_.clear_access_channels(websocketpp::log::alevel::frame_payload);
    server_.clear_access_channels(websocketpp::log::alevel::frame_header);
    server_.set_open_handler(bind(&MockWSServer::on_open, this, _1));
    server_.set_close_handler(bind(&MockWSServer::on_close, this, _1));
    server_.set_message_handler(bind(&MockWSServer::on_message, this, _1, _2));
    server_.set_tls_init_handler(bind(&MockWSServer::on_tls_init, _1));
    server_port_ = port;
    try {
        server_.listen(server_port_);
    } catch (const std::exception& e) {
        YIO_LOG_WARN("Can't run MockWSServer on port: " << server_port_);
        throw;
    }
    YIO_LOG_INFO("MockWSServer uses port " << server_port_);
    server_thread_ = std::thread(&MockWSServer::startServer, this);
}
void MockWSServer::send(const std::string& msg) {
    std::unique_lock<std::mutex> lock(mutex_);
    for (auto const& it : connections_) {
        auto conn = server_.get_con_from_hdl(it);
        if (conn->get_state() == websocketpp::session::state::closed || conn->get_state() == websocketpp::session::state::closing) {
            continue;
        }
        server_.send(it, msg, websocketpp::frame::opcode::text);
    }
}
void MockWSServer::sendBinary(const std::string& msg) {
    std::unique_lock<std::mutex> lock(mutex_);
    for (auto const& it : connections_) {
        server_.send(it, msg, websocketpp::frame::opcode::binary);
    }
}
void MockWSServer::closeConnections() {
    std::unique_lock<std::mutex> lock(mutex_);
    for (auto const& it : connections_) {
        server_.close(it, websocketpp::close::status::going_away, "idle");
        YIO_LOG_INFO("Closed connection " << it.lock().get());
    }
}
int MockWSServer::getPort() const {
    return server_port_;
}
int MockWSServer::getConnectionOpenedCnt() {
    std::unique_lock<std::mutex> lock(mutex_);
    return connection_opened_cnt;
}
int MockWSServer::getConnectionClosedCnt() {
    std::unique_lock<std::mutex> lock(mutex_);
    return connection_closed_cnt;
}
unsigned long MockWSServer::getCurrentConnectionCnt() {
    std::unique_lock<std::mutex> lock(mutex_);
    return connections_.size();
}

void MockWSServer::waitUntilConnections(int opened, int closed)
{
    YIO_LOG_INFO("MockWSServer Waiting for connection ts: " << time(nullptr));
    std::unique_lock<std::mutex> lock(mutex_);
    YIO_LOG_INFO("MockWSServer for connection: got mutex. ts: " << time(nullptr));
    while (connection_opened_cnt < opened || connection_closed_cnt < closed) {
        connectedWakeupVar_.wait_until(lock, std::chrono::steady_clock::now() + std::chrono::milliseconds(100));
    }
    YIO_LOG_INFO("MockWSServer for connection - DONE ts: " << time(nullptr));
}
