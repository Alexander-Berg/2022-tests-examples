#pragma once

#include <websocketpp/server.hpp>
#include <websocketpp/config/asio.hpp>

#include <set>

namespace quasar {
    namespace TestUtils {

        class MockWSServer {
        public:
            using OnMessage = std::function<void(const std::string&)>;
            using ConnectionPtr = websocketpp::server<websocketpp::config::asio_tls>::connection_ptr;
            using OnOpen = std::function<void(ConnectionPtr)>;

            OnMessage onMessage;
            OnMessage onBinaryMessage;
            OnOpen onOpen;
            ~MockWSServer();
            explicit MockWSServer(int port);
            void send(const std::string& msg);
            void sendBinary(const std::string& msg);
            void closeConnections();
            int getPort() const;
            int getConnectionOpenedCnt();
            int getConnectionClosedCnt();
            unsigned long getCurrentConnectionCnt();
            void waitUntilConnections(int opened, int closed);

        private:
            using con_list = std::set<websocketpp::connection_hdl, std::owner_less<websocketpp::connection_hdl>>;
            using server = websocketpp::server<websocketpp::config::asio_tls>;
            using message_ptr = websocketpp::config::asio::message_type::ptr;
            using context_ptr = websocketpp::lib::shared_ptr<websocketpp::lib::asio::ssl::context>;

            std::thread server_thread_;
            server server_;
            con_list connections_;
            int connection_opened_cnt;
            int connection_closed_cnt;
            int server_port_;
            std::mutex mutex_;
            std::condition_variable connectedWakeupVar_;

            void startServer();

            void on_message(websocketpp::connection_hdl hdl, message_ptr msg);

            void on_open(websocketpp::connection_hdl hdl);

            void on_close(websocketpp::connection_hdl hdl);
            static context_ptr on_tls_init(websocketpp::connection_hdl hdl);
        };
    } // namespace TestUtils
} // namespace quasar
