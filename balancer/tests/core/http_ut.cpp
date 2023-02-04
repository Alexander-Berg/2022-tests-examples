#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/env.h>

#include <balancer/serval/contrib/cno/hpack.h>
#include <balancer/serval/tests/helpers.h>

#include <util/generic/scope.h>

#define BOTH_VERSIONS(N) Y_UNIT_TEST_TEMPLATE(N, struct { bool useH2; }, {false}, {true})

static NSv::TThreadLocalRoot TLSRoot;

Y_UNIT_TEST_SUITE(Hx) {
    BOTH_VERSIONS(Pipelining) {
        TString state = "S";
        ServerClient([&](NSv::IStreamPtr s) {
            state += "H";
            Y_DEFER { state += "R"; };
            return cone::yield() && cone::yield() && cone::yield()
                && s->WriteHead(200) && s->Write("OK\n") && s->Close();
        }, [&](NSv::IConnection& client) {
            if (useH2) {
                auto bad = client.Request({"GET", "/", {/* no :scheme and :authority -- stream error in h2 */}});
                UNIT_ASSERT(bad && !bad->Head());
            }
            auto rsp1 = client.Request({"GET", "/a", {{":scheme", "http"}, {":authority", "localhost"}}});
            auto rsp2 = client.Request({"GET", "/b", {{":scheme", "http"}, {":authority", "localhost"}}});
            EXPECT_RESPONSE(rsp1, 200, "OK\n");
            EXPECT_RESPONSE(rsp2, 200, "OK\n");
        }, useH2);
        UNIT_ASSERT_VALUES_EQUAL(state, TString("SHHRR")); // not SHRHR
    }

    BOTH_VERSIONS(GracefulShutdown) {
        auto chan = SocketPair();
        bool done = false;
        cone::event e;
        cone::guard s = [&, fd = std::move(chan.first)]() mutable {
            Y_DEFER { done = true; };
            return NSv::Serve(fd, [&](NSv::IStreamPtr s) {
                return e.wait() && s->WriteHead(200) && s->Write("OK\n") && s->Close();
            }, nullptr);
        };
        auto client = NSv::H2Client(chan.second, nullptr, {.ForceH2 = useH2});
        auto rsp = client->Request({"GET", "/", {{":authority", "localhost"}, {":scheme", "http"}}});
        UNIT_ASSERT(cone::yield() && cone::yield() && cone::yield()); // actually write the request
        auto rsp2 = client->Request({"GET", "/", {{":authority", "localhost"}, {":scheme", "http"}}});
        s->cancel();
        UNIT_ASSERT(cone::yield() && cone::yield() && cone::yield()); // server should not stop yet
        if (useH2) {
            // Already know this request is rejected thanks to a GOAWAY frame. In h1 mode,
            // this would block because the error only happens after the socket is closed.
            UNIT_ASSERT(rsp2 && !rsp2->Head());
            UNIT_ASSERT_VALUES_EQUAL(mun_errno, ECONNRESET);
        }
        // This could also be done by sending a request with payload and only closing it here,
        // but then the second `Request` would block in h1 mode due to not being head-of-line.
        e.wake();
        EXPECT_RESPONSE(rsp, 200, "OK\n");
        UNIT_ASSERT(cone::yield() && cone::yield() && cone::yield());
        UNIT_ASSERT(done);
        if (!useH2) {
            UNIT_ASSERT(rsp2 && !rsp2->Head());
            UNIT_ASSERT_VALUES_EQUAL(mun_errno, ECONNRESET);
        }
    }

    Y_UNIT_TEST_TEMPLATE(H2Cancelled, struct { bool afterStart; }, {false}, {true}) {
        auto chan = SocketPair();
        TString state = "S";
        cone::guard s = [&, fd = std::move(chan.first)]() mutable {
            Y_DEFER { state += "F"; };
            return NSv::Serve(fd, [&](NSv::IStreamPtr s) {
                state += "H";
                if (!cone::yield() || !cone::yield() || !cone::yield() || !cone::yield()) {
                    state += "C";
                    return false;
                }
                state += "R";
                return s->WriteHead(200) && s->Write("OK\n") && s->Close();
            }, nullptr);
        };
        auto client = NSv::H2Client(chan.second, nullptr, {.ForceH2 = true});
        UNIT_ASSERT(cone::yield() && cone::yield());
        // Now in a stable state: all coroutines but this one are waiting for I/O.
        auto rsp = client->Request({"GET", "/", {{":authority", "localhost"}, {":scheme", "http"}}});
        if (afterStart) {
            UNIT_ASSERT(cone::yield() && cone::yield());
        } else {
            // Yield once to flush the write buffer and get scheduled before the server can react.
            UNIT_ASSERT(cone::yield());
        }
        UNIT_ASSERT_VALUES_EQUAL(state, TString(afterStart ? "SH" : "S"));
        // RST_STREAM (length=4, flags=0, stream=1): RST_CANCEL
        UNIT_ASSERT(chan.second.Write({"\x00\x00\x04" "\x03" "\x00" "\x00\x00\x00\x01" "\x00\x00\x00\x08", 13}));
        UNIT_ASSERT(cone::yield() && cone::yield() && cone::yield() && cone::yield());
        UNIT_ASSERT_VALUES_EQUAL(state, TString(afterStart ? "SHC" : "S"));
    }

    BOTH_VERSIONS(ServerCloseBeforeWrite) {
        auto chan = SocketPair();
        cone::guard s = [&, fd = std::move(chan.first)]() mutable {
            cone::event sync;
            // Unlike `Serve`, this does not initiate a graceful shutdown upon destruction,
            // so the request will not be fully processed.
            auto conn = NSv::H2Server(fd, [&](NSv::IStreamPtr s) {
                if (!s->WriteHead(200))
                    return false;
                sync.wake();
                return cone::sleep_for(std::chrono::seconds(10));
            }, nullptr);
            return sync.wait() && cone::yield(); // allow the client to consume head
        };
        auto client = NSv::H2Client(chan.second, nullptr, {.ForceH2 = useH2});
        auto rsp = client->Request({"GET", "/", {{":authority", "localhost"}, {":scheme", "http"}}});
        UNIT_ASSERT(rsp);
        auto head = rsp->Head();
        UNIT_ASSERT(head);
        UNIT_ASSERT_VALUES_EQUAL(head->Code, 200);
        UNIT_ASSERT(!rsp->Read());
        UNIT_ASSERT_VALUES_EQUAL(mun_errno, ECONNRESET);
    }

    // While h2 does not allow the `connection` header, its special `close` value is
    // translated into a GOAWAY frame, so behavior is the same as for h1, except the client
    // knows how many requests the server will process before terminating.
    BOTH_VERSIONS(ServerConnectionClose) {
        ServerClient([&](NSv::IStreamPtr s) {
            return s->WriteHead({200, {{"connection", "close"}}}) && s->Write("OK\n") && s->Close();
        }, [&](NSv::IConnection& client) {
            UNIT_ASSERT(client.IsOpen());
            EXPECT_RESPONSE(client.Request({"GET", "/", {{":authority", "localhost"}, {":scheme", "http"}}}), 200, "OK\n");
            UNIT_ASSERT(!client.IsOpen());
            UNIT_ASSERT(!client.Request({"GET", "/", {{":authority", "localhost"}, {":scheme", "http"}}}));
            UNIT_ASSERT_VALUES_EQUAL(mun_errno, EPIPE);
        }, useH2);
    }

    // Correction: the above comment is only true for the server side.
    Y_UNIT_TEST(H1ClientConnectionClose) {
        ServerClient([&](NSv::IStreamPtr s) {
            return s->WriteHead(200) && s->Write("OK\n") && s->Close();
        }, [&](NSv::IConnection& client) {
            UNIT_ASSERT(client.IsOpen());
            auto req = client.Request({"GET", "/", {{":authority", "localhost"}, {":scheme", "http"}, {"connection", "close"}}});
            EXPECT_RESPONSE(req, 200, "OK\n");
            UNIT_ASSERT(!client.IsOpen());
            UNIT_ASSERT(!client.Request({"GET", "/", {{":authority", "localhost"}, {":scheme", "http"}}}));
            UNIT_ASSERT_VALUES_EQUAL(mun_errno, EPIPE);
        });
    }

    BOTH_VERSIONS(ClientGracefulShutdown) {
        ServerClient([&](NSv::IStreamPtr s) {
            return s->WriteHead(200) && s->Write("OK\n") && s->Close();
        }, [&](NSv::IConnection& client) {
            UNIT_ASSERT(client.IsOpen());
            auto req = client.Request({"GET", "/", {{":authority", "localhost"}, {":scheme", "http"}}});
            UNIT_ASSERT(client.Shutdown());
            EXPECT_RESPONSE(req, 200, "OK\n");
            UNIT_ASSERT(!client.IsOpen());
            UNIT_ASSERT(!client.Request({"GET", "/", {{":authority", "localhost"}, {":scheme", "http"}}}));
            UNIT_ASSERT_VALUES_EQUAL(mun_errno, EPIPE);
        }, useH2);
    }

    Y_UNIT_TEST(H2CancelOnStreamDrop) {
        cone::event onReqStart;
        cone::event onReqDone;
        ServerClient([&](NSv::IStreamPtr) {
            Y_DEFER { onReqDone.wake(); };
            onReqStart.wake();
            UNIT_ASSERT(!cone::sleep_for(std::chrono::seconds(1)));
            return false;
        }, [&](NSv::IConnection& client) {
            {
                auto req = client.Request({"GET", "/", {{":authority", "localhost"}, {":scheme", "http"}}});
                UNIT_ASSERT(req);
                UNIT_ASSERT(onReqStart.wait());
            }
            UNIT_ASSERT(onReqDone.wait());
        }, true);
    }

    BOTH_VERSIONS(PayloadBufferOverflow) {
        ServerClient([&](NSv::IStreamPtr s) {
            // Do not consume any payload.
            return cone::sleep_for(std::chrono::milliseconds(500))
                && s->WriteHead(200) && s->Write("OK\n") && s->Close();
        }, [&](NSv::IConnection& client) {
            auto rsp = client.Request({"POST", "/", {{":authority", "localhost"}, {":scheme", "http"}}}, true);
            UNIT_ASSERT(rsp);
            // Overflow the read buffer, forcing the server's reader coroutine to block until
            // some payload is consumed. This means it can no longer detect connection
            // termination, because it is not reading from the socket.
            bool wrote = rsp->Write(TString(512 * 1024, 'a'));
            // In h2 mode, closing the stream at server side cancels it, aborting the write.
            UNIT_ASSERT(useH2 ? !wrote && mun_errno == EPIPE : wrote);
        }, useH2);
        // Should terminate as soon as the sleep finishes at the latest.
    }

    Y_UNIT_TEST(H1Upgrade) {
        for (bool close : {false, true}) {
            ServerClient([&](NSv::IStreamPtr s) {
                auto rqh = s->Head();
                if (!rqh)
                    return false;
                auto it = rqh->find("upgrade");
                if (it == rqh->end() || it->second != "echo")
                    return s->WriteHead(400) && s->Close();
                if (!s->WriteHead({101, {{"upgrade", "echo"}, {"connection", "upgrade"}}}))
                    return false;
                while (auto chunk = s->Read())
                    if (!(*chunk && *chunk != "end" ? s->Write(*chunk) && s->Consume(chunk->size()) : s->Close()))
                        return false;
                return false;
            }, [&](NSv::IConnection& client) {
                auto rsp = client.Request({"GET", "/", {{"upgrade", "echo"}, {"connection", "upgrade"}}}, true);
                UNIT_ASSERT(rsp);
                auto rsh = rsp->Head();
                UNIT_ASSERT(rsh);
                UNIT_ASSERT_VALUES_EQUAL(rsh->Code, 101);
                EXPECT_HEADER(*rsh, "upgrade", "echo");
                UNIT_ASSERT(rsp->Write("hello, world"));
                UNIT_ASSERT_VALUES_EQUAL(rsp->Read(), "hello, world");
                UNIT_ASSERT(rsp->Consume(12));
                if (close) {
                    UNIT_ASSERT(rsp->Close());
                } else {
                    UNIT_ASSERT(rsp->Write("end"));
                    UNIT_ASSERT_VALUES_EQUAL(rsp->Read(), TStringBuf{});
                }
                UNIT_ASSERT(!client.IsOpen());
            });
        }
    }

    Y_UNIT_TEST(Headache) {
        // After looking through TStringBuf's code to find the needed methods, I feel
        // like my head is going to *split* in half. Ha ha ha, get it?
        NSv::THead h1{"GET", "/asd", {}};
        NSv::THead h2{"GET", "/asd?qwe", {}};
        NSv::THead h3{"GET", "/asd#zxc", {}};
        NSv::THead h4{"GET", "/asd?qwe#zxc", {}};
        NSv::THead hc{"GET", "/", {
            {"cookie", "a"},
            {"cookie", ""},
            {"cookie", "b="},
            {"cookie", "=c"},
            {"cookie", ";;=;;; d=e; ; ;;=;;;f"},
        }};
        UNIT_ASSERT_VALUES_EQUAL(h1.Path(), "/asd");
        UNIT_ASSERT_VALUES_EQUAL(h2.Path(), "/asd");
        UNIT_ASSERT_VALUES_EQUAL(h3.Path(), "/asd");
        UNIT_ASSERT_VALUES_EQUAL(h4.Path(), "/asd");
        UNIT_ASSERT_VALUES_EQUAL(h1.Query(), "");
        UNIT_ASSERT_VALUES_EQUAL(h2.Query(), "qwe");
        UNIT_ASSERT_VALUES_EQUAL(h3.Query(), "");
        UNIT_ASSERT_VALUES_EQUAL(h4.Query(), "qwe");
        UNIT_ASSERT_VALUES_EQUAL(h1.Fragment(), "");
        UNIT_ASSERT_VALUES_EQUAL(h2.Fragment(), "");
        UNIT_ASSERT_VALUES_EQUAL(h3.Fragment(), "zxc");
        UNIT_ASSERT_VALUES_EQUAL(h4.Fragment(), "zxc");
        UNIT_ASSERT_VALUES_EQUAL(ParseCookie(hc), (NSv::TCookieVector{
            {"a", Nothing()},
            {"b", ""},
            {"", "c"},
            {"d", "e"},
            {"f", Nothing()},
        }));
    }

    Y_UNIT_TEST(Fairness) {
        // This test checks the order of writes in h2 mode.
        ServerClient([&](NSv::IStreamPtr& s) {
            // Unimportant.
            return s->WriteHead(200) && s->Close();
        }, [&](NSv::IConnection& client) {
            TString looong(1024, 'b');
            // So here's the scenario.
            // 1. A ton of requests are sent. They are big enough so that the limit on the
            //    write buffer is hit before the limit on the number of concurrent streams.
            //    E.g. at 32KiB buffers and 100 concurrent streams, that's >= 328 bytes each.
            cone::guard a = [&]() {
                // The `a` header will reuse an entry in the HPACK dynamic table. The `b`
                // one is pure padding, which is why it should not be indexed.
                NSv::THead h{"GET", "/", {{":authority", "localhost"}, {":scheme", "http"}, {"a", "a"}, {"b", looong}}};
                ((cno_header_t*)&h.back())->flags |= CNO_HEADER_NOT_INDEXED;
                for (size_t i = 0; i < 200; i++)
                    UNIT_ASSERT(client.Request(h));
                return true;
            };
            // 2. This coroutine waits for the buffer to drain somewhat. Before that happens,
            //    another coroutine attempts to also send a request. It blocks on the mutex
            //    (that ensures atomicity of frames) instead.
            cone::guard b = [&]() {
                // The `c` header is unique, so it displaces all entries in the HPACK dynamic
                // table by 1, changing the encoding of requests in the other coroutine.
                UNIT_ASSERT(client.Request({"GET", "/", {{":authority", "localhost"}, {":scheme", "http"}, {"c", "c"}}}));
                return true;
            };
            // 3. The write buffer drains and schedules the first coroutine to write more.
            UNIT_ASSERT(cone::yield());
            // 4. The coroutine finishes the write, and immediately sends another request.
            //    Meanwhile, the second coroutine has compressed the headers and is waiting
            //    on the mutex. This results in the writes being out of order, making that
            //    `a` header in the next request have an out-of-bounds HPACK dyntable index.
            UNIT_ASSERT(b->wait(cone::rethrow));
            // (The solution is a fair mutex, obviously.)
            UNIT_ASSERT(a->wait(cone::rethrow));
        }, true);
    }

    Y_UNIT_TEST(UDP) {
        auto ip = NSv::IP::Parse("[::1]", 19239);
        auto sk = NSv::TFile::Bind<SOCK_DGRAM>(ip, true);
        UNIT_ASSERT(sk);
        auto server = NSv::TestUDPServer(sk, [](NSv::IStreamPtr s) {
            auto head = s->Head();
            UNIT_ASSERT(head);
            UNIT_ASSERT_VALUES_EQUAL(head->Method, "GET");
            UNIT_ASSERT_VALUES_EQUAL(head->Path(), "/");
            EXPECT_HEADER(*head, "x-ok", "1");
            auto data = NSv::ReadFrom(*s);
            UNIT_ASSERT(data);
            return s->WriteHead({200, {{"x-ok", "1"}}}) && s->Write(*data) && s->Close();
        });
        UNIT_ASSERT(server);
        auto conn = NSv::TestUDPClient(ip);
        UNIT_ASSERT(conn);
        auto rsp1 = conn->Request({"GET", "/", {{"x-ok", "1"}}}, true);
        UNIT_ASSERT(rsp1);
        UNIT_ASSERT(rsp1->Write("OK\n"));
        UNIT_ASSERT(rsp1->Close());
        auto rsp2 = conn->Request({"GET", "/", {{"x-ok", "1"}}});
        EXPECT_RESPONSE(rsp1, 200, "OK\n", {"x-ok", "1"});
        EXPECT_RESPONSE(rsp2, 200, "", {"x-ok", "1"});
    }

    Y_UNIT_TEST(TCP) {
        auto chan = SocketPair();
        cone::guard server = [&, fd = std::move(chan.first)]() mutable {
            return NSv::RawTCPServer(fd, [&](NSv::IStreamPtr s) {
                return s->WriteHead(200) && s->Write("Hello, World!") && s->Close();
            })->Wait();
        };
        auto fd = std::move(chan.second);
        auto client = NSv::RawTCPClient(fd);
        EXPECT_RESPONSE(client->Request({"CONNECT", "localhost"}), 200, "Hello, World!");
    }
}
