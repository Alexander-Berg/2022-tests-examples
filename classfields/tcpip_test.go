package ssh

import (
	"bytes"
	"github.com/miekg/dns"
	gossh "golang.org/x/crypto/ssh"
	"io/ioutil"
	"net"
	"strconv"
	"strings"
	"testing"
	"time"
)

var (
	sampleServerResponse = []byte("Hello world")

	resolver      = "77.88.8.8:53"
	autoRuDomain  = "auto.ru"
	autoRuIp      = "2a02:6b8::188"
	invalidDomain = "blablabla"
)

func sampleSocketServer() net.Listener {
	l := newLocalListener()

	go func() {
		conn, err := l.Accept()
		if err != nil {
			return
		}
		conn.Write(sampleServerResponse)
		conn.Close()
	}()

	return l
}

func newTestSessionWithForwarding(t *testing.T, forwardingEnabled bool) (net.Listener, *gossh.Client, func()) {
	l := sampleSocketServer()

	_, client, cleanup := newTestSession(t, &Server{
		Handler: func(s Session) {},
		LocalPortForwardingCallback: func(ctx Context, destinationHost string, destinationPort uint32) Verdict {
			addr := net.JoinHostPort(destinationHost, strconv.FormatInt(int64(destinationPort), 10))
			if addr != l.Addr().String() {
				panic("unexpected destinationHost: " + addr)
			}
			if forwardingEnabled {
				return Verdict{
					Choice: TCP,
				}
			} else {
				return Verdict{
					Choice: NO,
					Reason: "",
				}
			}
		},
	}, nil)

	return l, client, func() {
		cleanup()
		l.Close()
	}
}

func TestLocalPortForwardingWorks(t *testing.T) {
	t.Parallel()

	l, client, cleanup := newTestSessionWithForwarding(t, true)
	defer cleanup()

	conn, err := client.Dial("tcp", l.Addr().String())
	if err != nil {
		t.Fatalf("Error connecting to %v: %v", l.Addr().String(), err)
	}
	result, err := ioutil.ReadAll(conn)
	if err != nil {
		t.Fatal(err)
	}
	if !bytes.Equal(result, sampleServerResponse) {
		t.Fatalf("result = %#v; want %#v", result, sampleServerResponse)
	}
}

func TestLocalPortForwardingRespectsCallback(t *testing.T) {
	t.Parallel()

	l, client, cleanup := newTestSessionWithForwarding(t, false)
	defer cleanup()

	_, err := client.Dial("tcp", l.Addr().String())
	if err == nil {
		t.Fatalf("Expected error connecting to %v but it succeeded", l.Addr().String())
	}
	if !strings.Contains(err.Error(), "ssh: rejected: administratively prohibited") {
		t.Fatalf("Expected permission error but got %#v", err)
	}
}

func TestResolveIP(t *testing.T) {
	t.Parallel()

	// Даем домен - должен вернуть айпишник
	ip, err := resolveIP(autoRuDomain, resolver)
	if err != nil {
		t.Fatalf("Cannot resolve '%s': %v", autoRuDomain, err)
	}
	if ip != autoRuIp {
		t.Fatalf("Got %s ip from resolver for '%s'. Expected: %s", ip, autoRuDomain, autoRuIp)
	}

	// Даем айпишник - должно вернуть его же
	ip, err = resolveIP(autoRuIp, resolver)
	if err != nil {
		t.Fatalf("Cannot resolve '%s': %v", autoRuIp, err)
	}
	if ip != autoRuIp {
		t.Fatalf("Got %s ip from resolver for '%s'. Expected: %s", ip, autoRuIp, autoRuIp)
	}

	// Даем невалидный домен
	_, err = resolveIP(invalidDomain, resolver)
	if err == nil {
		t.Fatalf("Expected error resolve '%s' but it succeeded", invalidDomain)
	}
	if !strings.Contains(err.Error(), "empty answer from resolver") {
		t.Fatalf("Expected 'empty answer' error but got %v", err)
	}
}

func TestResolveIPMoreThan512Bytes(t *testing.T) {
	s := &dns.Server{Addr: "[::1]:0", Net: "udp"}
	dns.HandleFunc(".", func(w dns.ResponseWriter, r *dns.Msg) {
		msg := dns.Msg{}
		msg.SetReply(r)

		msg.Authoritative = true
		for i := 0; i < 20; i++ {
			msg.Answer = append(msg.Answer, &dns.AAAA{
				Hdr: dns.RR_Header{
					Name:   dns.Fqdn(autoRuDomain),
					Rrtype: dns.TypeAAAA,
					Class:  dns.ClassINET,
					Ttl:    60,
				},
				AAAA: net.ParseIP(autoRuIp),
			})
		}
		w.WriteMsg(&msg)
	})

	go func() {
		err := s.ListenAndServe()
		if err != nil {
			t.Error(err)
		}
	}()
	defer s.Shutdown()

	for s.PacketConn == nil {
		time.Sleep(100 * time.Millisecond)
	}

	ip, err := resolveIP(autoRuDomain, s.PacketConn.LocalAddr().String())
	if err != nil {
		t.Fatalf("Cannot resolve '%s': %v", autoRuIp, err)
	}

	if ip != autoRuIp {
		t.Fatalf("Got %s ip from resolver for '%s'. Expected: %s", ip, autoRuDomain, autoRuIp)
	}
}
