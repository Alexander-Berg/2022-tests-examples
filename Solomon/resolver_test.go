package resolver

import (
	"fmt"
	"net"
	"reflect"
	"strings"
	"testing"
	"time"
)

var yaReqs = []*Request{
	{Name: "www.ya.ru"},
	{Name: "87.250.250.242"},
	{"www.ya.ru", CNAME},
	{"ya.ru", NS},
	{"ya.ru", TXT},
	{"ya.ru", MX},
	{"2a02:6b8::2:242", PTR},
	{"www.ya.ru", A},
}

func respYaChecker(t *testing.T, recs []*Record) {
	for _, rec := range recs {
		switch rec.Type {
		case NONE:
			t.Errorf("Resolver bad response type = NONE, should not happen")
		case CNAME:
			if rec.RDATA != "ya.ru" {
				t.Errorf("Resolver bad response = %v, want = ya.ru", rec.Name)
			}
		case NS:
			netNSs := rec.RDATA.([]*net.NS)
			if len(netNSs) == 0 {
				t.Errorf("Resolver bad response: no NS RRs found")
			}
			for _, netNS := range netNSs {
				if netNS.Host != "ns1.yandex.ru" && netNS.Host != "ns2.yandex.ru" {
					t.Errorf("Resolver bad response = %v, want = ns{1,2}.yandex.ru", netNS.Host)
				}
			}
		case TXT:
			spfFound := false
			for _, txt := range rec.RDATA.([]string) {
				if strings.Contains(txt, "spf") {
					spfFound = true
				}
			}
			if !spfFound {
				t.Errorf("Resolver bad response = %v, want = '.+spf.+'", rec.RDATA)
			}
		case MX:
			netMXs := rec.RDATA.([]*net.MX)
			if len(netMXs) == 0 {
				t.Errorf("Resolver bad response: no MX RRs found")
			}
			for _, netMX := range netMXs {
				if netMX.Host != "mx.yandex.ru" {
					t.Errorf("Resolver bad response = %v, want = mx.yandex.ru", netMX.Host)
				}
			}
		case PTR:
			ptrs := rec.RDATA.([]string)
			if len(ptrs) == 0 {
				t.Errorf("Resolver bad response: no PTR RRs found")
			}
			for _, ptr := range ptrs {
				if ptr != "ya.ru" {
					t.Errorf("Resolver bad response = %v, want = ya.ru", ptr)
				}
			}
		case A:
			netIPs := rec.RDATA.([]net.IP)
			if len(netIPs) == 0 {
				t.Errorf("Resolver bad response: no A RRs found")
			}
			for _, netIP := range netIPs {
				if netIP.String() != "2a02:6b8::2:242" && netIP.String() != "87.250.250.242" {
					t.Errorf("Resolver bad response = %v, want = 2a02:6b8::2:242 or 87.250.250.242", netIP)
				}
			}
		default:
			t.Errorf("Resolver bad response = %v: no such type", rec.Type)
		}
	}
}

func resolverYaOk(req Request) (interface{}, error) {
	var err error

	rec := &Record{
		Name: req.Name,
		Type: req.Type,
	}
	switch req.Type {
	case PTR:
		rec.RDATA = []string{"ya.ru"}
	case A:
		rec.RDATA = []net.IP{net.ParseIP("2a02:6b8::2:242"), net.ParseIP("87.250.250.242")}
	case CNAME:
		rec.RDATA = "ya.ru"
	case MX:
		rec.RDATA = []*net.MX{{Host: "mx.yandex.ru", Pref: 10}}
	case NS:
		rec.RDATA = []*net.NS{{Host: "ns1.yandex.ru"}, {Host: "ns2.yandex.ru"}}
	case TXT:
		rec.RDATA = []string{"v=spf1 redirect=_spf.yandex.ru", "e1ee8bed6c0a3f9cd6764b2bc36377fa90b6045461e934966ee92b11e9d9e06e"}
	}
	rec.Error = err
	return rec, err
}

func resolverBlocking(req Request) (interface{}, error) {
	rec := &Record{
		Name: req.Name,
		Type: req.Type,
	}
	rec.RDATA = reflect.Indirect(reflect.New(typeTypes[req.Type])).Interface()
	rec.Error = fmt.Errorf("resolving is blocked")
	return rec, rec.Error
}

func TestResolverFull(t *testing.T) {
	goodCacheTime := time.Second
	badCacheTime := time.Second
	prefetchTime := time.Second
	cleanUpInterval := 5 * time.Second
	cacheSize := 1000
	workers := 5
	serveStale := true
	fixDots := true
	verboseLevel := 0

	r := NewResolver(goodCacheTime, badCacheTime, prefetchTime, cleanUpInterval,
		cacheSize, workers,
		serveStale, fixDots,
		verboseLevel)
	r.resolver = resolverYaOk

	for i := 0; i < 3; i++ {
		resps := r.Resolv(yaReqs, nil)
		respYaChecker(t, resps)
	}
	r.Destroy()
}

func TestResolverSimple(t *testing.T) {
	goodCacheTime := time.Second
	badCacheTime := time.Second
	prefetchTime := time.Second
	cleanUpInterval := 5 * time.Second
	cacheSize := 1000
	workers := 5
	serveStale := true
	fixDots := true
	verboseLevel := 0

	r := NewResolver(goodCacheTime, badCacheTime, prefetchTime, cleanUpInterval,
		cacheSize, workers,
		serveStale, fixDots,
		verboseLevel)
	r.resolver = resolverYaOk

	req := []string{
		"ya.ru",
		"www.ya.ru",
		"87.250.250.242",
		"2a02:6b8::2:242",
	}
	ips1 := []string{"2a02:6b8::2:242", "87.250.250.242"}
	ips2 := []string{"87.250.250.242", "2a02:6b8::2:242"}
	names := []string{"ya.ru"}

	for i := 0; i < 3; i++ {
		resp := r.ResolvSimple(req)
		if len(resp) != 4 {
			t.Errorf("Resolver bad simple resolv: response length = %d, want = 4", len(resp))
		}
		for r, rec := range resp {
			if (r == "87.250.250.242" && !reflect.DeepEqual(rec, names)) ||
				(r == "2a02:6b8::2:242" && !reflect.DeepEqual(rec, names)) ||
				(r == "ya.ru" && !reflect.DeepEqual(rec, ips1) && !reflect.DeepEqual(rec, ips2)) ||
				(r == "www.ya.ru" && !reflect.DeepEqual(rec, ips1) && !reflect.DeepEqual(rec, ips2)) {

				t.Errorf("Resolver bad simple resolv: request = %v, response = %v", r, rec)
			}
		}
	}
	r.Destroy()
}

func TestResolverDump(t *testing.T) {
	goodCacheTime := time.Second
	badCacheTime := time.Second
	prefetchTime := time.Second
	cleanUpInterval := 5 * time.Second
	cacheSize := 1000
	workers := 5
	serveStale := true
	fixDots := true
	verboseLevel := 0

	r := NewResolver(goodCacheTime, badCacheTime, prefetchTime, cleanUpInterval,
		cacheSize, workers,
		serveStale, fixDots,
		verboseLevel)
	r.resolver = resolverYaOk

	_ = r.Resolv(yaReqs, nil)
	rDump, err := r.Dump(false)
	if err != nil {
		t.Errorf("Resolver bad dump: %v", err)
	}
	r.Purge()
	if r.cache.Len() != 0 {
		t.Errorf("Resolver bad purge: len(cache) = %d", r.cache.Len())
	}
	err = r.Restore(rDump)
	if err != nil {
		t.Errorf("Resolver bad restore: %v", err)
	}
	r.resolver = resolverBlocking
	resps := r.Resolv(yaReqs, nil)
	respYaChecker(t, resps)

	r.Destroy()
}

func TestResolverStale(t *testing.T) {
	goodCacheTime := time.Second
	badCacheTime := time.Microsecond
	prefetchTime := time.Second
	cleanUpInterval := 5 * time.Second
	cacheSize := 1000
	workers := 5
	serveStale := true
	fixDots := true
	verboseLevel := 0

	rDump := []byte(`[{
		"k":{"n":"ya.ru","t":"A"},
		"v":{"n":"ya.ru","t":"A","r":["2a02:6b8::2:242"],"e":null},
		"t":0}]`)
	r := NewResolver(goodCacheTime, badCacheTime, prefetchTime, cleanUpInterval,
		cacheSize, workers,
		serveStale, fixDots,
		verboseLevel)
	r.resolver = resolverBlocking
	err := r.Restore(rDump)
	if err != nil {
		t.Errorf("Resolver bad serve stale: %v", err)
	}

	resp := r.ResolvSimple([]string{"ya.ru"})
	if len(resp) != 1 {
		t.Errorf("Resolver bad serve stale: %v, want = map[\"ya.ru\":[\"2a02:6b8::2:242\"]]", resp)
	}
	if v, ok := resp["ya.ru"]; !ok {
		t.Errorf("Resolver bad serve stale: ya.ru is not in the response")
	} else if len(v) != 1 || v[0] != "2a02:6b8::2:242" {
		t.Errorf("Resolver bad serve stale: [2a02:6b8::2:242] is not in the response, %v", v)
	}

	r.cache.ServeStale = false
	// wait 1 ms to badCacheTime to expire
	time.Sleep(time.Millisecond)
	resp = r.ResolvSimple([]string{"ya.ru"})
	if len(resp) != 1 {
		t.Errorf("Resolver bad serve stale: %v, want = map[\"ya.ru\":[]]", resp)
	}
	if v, ok := resp["ya.ru"]; !ok {
		t.Errorf("Resolver bad serve stale: ya.ru is not in the response")
	} else if len(v) != 0 {
		t.Errorf("Resolver bad serve stale: response is not empty, %v", v)
	}

	r.Destroy()
}
