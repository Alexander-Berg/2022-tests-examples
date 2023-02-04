package ipdc

import (
	"net"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"a.yandex-team.ru/solomon/libs/go/uhttp"
)

/*
5.45.193.168/29	542	ТП ЦОД КрымКом	backbone
5.45.197.0/25	802	Сасово-2.11	backbone
5.45.197.128/25	802	Сасово-2.11	backbone
5.45.198.0/23	640	Ивантеевка	backbone
95.108.249.0/24	2600	Мытищи	backbone
95.108.252.0/23	640	Мытищи	backbone

95.108.252.0/24	640	Мытищи	backbone
95.108.253.0/24	640	Мытищи	backbone
95.108.254.0/28 640     Мытищи	backbone
95.108.255.0/30	595	Мытищи	backbone
2a02:6b8:fc2c::/48	788	Владимир-3	fastbone
2a02:6b8:fc2c::/57	788	Владимир-3	fastbone
2a02:6b8:fc2c::/59	788	Владимир-3	fastbone
2a02:6b8:fc2c:01::/60	788	Владимир-3	fastbone
2a02:6b8:fc2c:80::/57	788	Владимир-3	fastbone
2a02:6b8:fc2c:100::/57	788	Владимир-3	fastbone
2a02:6b8:fc2c:180::/57	788	Владимир-3	fastbone
2a02:6b8:fc2c:200::/57	788	Владимир-3	fastbone
2a02:6b8:fc2c:280::/57	788	Владимир-3	fastbone
2a02:6b8:fc2c:300::/57	788	Владимир-3	fastbone
2a02:6b8:fc2c:380::/57	788	Владимир-3	fastbone

*/
var data = []byte{
	31, 139, 8, 8, 145, 5, 187, 97, 0, 3, 113, 119, 101, 0, 157, 209, 75, 78, 195, 48, 16, 128, 225, 181, 123, 138, 92, 160, 206, 120,
	252, 136, 157, 219, 36, 17, 221, 32, 193, 2, 14, 80, 216, 161, 238, 16, 11, 84, 33, 16, 44, 88, 71, 21, 72, 17, 148, 158, 97, 124,
	35, 38, 66, 2, 4, 41, 36, 118, 182, 223, 63, 99, 57, 86, 26, 43, 85, 208, 82, 57, 159, 99, 16, 214, 160, 160, 7, 186, 203, 232,
	145, 110, 233, 42, 163, 117, 92, 198, 21, 109, 105, 77, 59, 218, 138, 186, 106, 14, 235, 227, 163, 131, 153, 253, 232, 10, 9, 57, 90, 225,
	129, 171, 123, 106, 227, 25, 171, 13, 237, 230, 40, 149, 26, 192, 10, 253, 120, 238, 251, 217, 90, 56, 3, 130, 174, 153, 181, 244, 22, 207,
	233, 153, 191, 13, 189, 80, 251, 229, 3, 107, 240, 18, 77, 232, 11, 35, 208, 1, 39, 55, 113, 197, 188, 139, 23, 212, 253, 166, 22, 191,
	13, 31, 148, 63, 168, 249, 131, 126, 74, 61, 90, 154, 94, 250, 140, 101, 214, 159, 127, 180, 101, 173, 65, 216, 96, 247, 204, 197, 10, 176,
	116, 181, 47, 23, 13, 54, 101, 153, 27, 47, 10, 239, 5, 93, 210, 43, 63, 219, 19, 117, 252, 3, 187, 184, 156, 107, 177, 168, 78, 78,
	7, 19, 91, 76, 79, 194, 196, 4, 20, 87, 14, 38, 86, 30, 82, 174, 167, 32, 45, 75, 219, 134, 105, 219, 48, 109, 155, 78, 219, 166,
	199, 110, 155, 189, 3, 37, 215, 105, 118, 22, 4, 0, 0,
}

var ips = map[string]struct {
	IP net.IP
	DC string
}{
	"GoodIPv6":   {net.ParseIP("2a02:6b8:fc2c:0:5cc1:30ff:fe44:8b04"), "vla"},
	"AbsentIPv4": {net.ParseIP("5.255.254.255"), ""},
	"LargeIPv4":  {net.ParseIP("195.108.255.3"), ""},
	"GoodIPv4":   {net.ParseIP("95.108.252.34"), "myt"},
	"EdgeIPv6":   {net.ParseIP("2a02:6b8:fc2c:3cf::ffff"), "vla"},
	"AbsentIPv6": {net.ParseIP("2a02:6b8:fc2d::1"), ""},
}

func TestIpDcGet(t *testing.T) {
	verboseLevel := 0

	i := NewIPDCWithDefaults(verboseLevel)
	//i.netListFile = "networklist-perdc.txt.gz"
	i.netListData = data
	for s, v := range ips {
		dc, err := i.GetDc(v.IP)
		if dc == "" && v.DC == "" {
			continue
		}
		if err != nil {
			t.Errorf("IpDc bad response for %s: %v", s, err)
		}
		if dc != v.DC {
			t.Errorf("IpDc bad dc for %s %v: got = '%v', want = '%v'", s, v.IP, dc, v.DC)
		}
	}
	i.Destroy()
}

func TestIpDcGetMany(t *testing.T) {
	verboseLevel := 0

	i := NewIPDCWithDefaults(verboseLevel)
	//i.netListFile = "networklist-perdc.txt.gz"
	i.netListData = data

	ipOnly := make([]net.IP, len(ips))
	dcOnly := make([]string, len(ips))
	j := 0
	for _, v := range ips {
		ipOnly[j] = v.IP
		dcOnly[j] = v.DC
		j++
	}

	dcs, err := i.GetDcMany(ipOnly)
	if err != nil {
		t.Errorf("IpDc bad get many: %v", err)
	}
	for idx, dc := range dcs {
		if dc != dcOnly[idx] {
			t.Errorf("IpDc get many bad result: get = %v, want = %v", dc, dcOnly[idx])
		}
	}
	i.Destroy()
}

func TestIpDcGetManyOne(t *testing.T) {
	verboseLevel := 0

	i := NewIPDCWithDefaults(verboseLevel)
	//i.netListFile = "networklist-perdc.txt.gz"
	i.netListData = data

	ip := net.ParseIP("2a02:6b8:fc2c:0:5cc1:30ff:fe44:8b04")
	dc := "vla"

	dcx, err := i.GetDcMany([]net.IP{ip})
	if err != nil {
		t.Errorf("IpDc bad get many: %v", err)
	}
	if dcx[0] != dc {
		t.Errorf("IpDc get many bad result: get = %v, want = %v", dcx[0], dc)
	}
	i.Destroy()
}

func TestIpDcDump(t *testing.T) {
	verboseLevel := 0
	fail := false

	i := NewIPDCWithDefaults(verboseLevel)
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if fail {
			w.WriteHeader(http.StatusForbidden)
			return
		}
		fail = true
		if _, err := w.Write(data); err != nil {
			t.Errorf("%v", err)
		}
	}))
	defer server.Close()
	i.rtClient = uhttp.NewClient(server.URL, "", nil, time.Second, true)
	_, _ = i.GetDc(net.ParseIP("1.1.1.1"))

	iDump, err := i.Dump(true)
	if err != nil {
		t.Errorf("IpDc bad dump: %v", err)
	}
	i.Purge()
	if i.cache.Len() != 0 {
		t.Errorf("IpDc bad purge: len(cache) = %d", i.cache.Len())
	}

	err = i.Restore(iDump)
	if err != nil {
		t.Errorf("IpDc bad restore: %v", err)
	}
	for s, v := range ips {
		dc, err := i.GetDc(v.IP)
		if dc == "" && v.DC == "" {
			continue
		}
		if err != nil {
			t.Errorf("IpDc bad response for %s: %v", s, err)
		}
		if dc != v.DC {
			t.Errorf("IpDc bad dc for %s %v: got = '%v', want = '%v'", s, v.IP, dc, v.DC)
		}
	}
	i.Destroy()
}

func BenchmarkIpDcGet(b *testing.B) {
	verboseLevel := 0

	i := NewIPDCWithDefaults(verboseLevel)
	//i.netListFile = "networklist-perdc.txt.gz"
	i.netListData = data
	_, _ = i.GetDc(net.ParseIP("1.1.1.1"))

	b.ResetTimer()
	for ix := 0; ix < b.N; ix++ {
		//_, _ = i.GetDc(ips["EdgeIPv6"].IP)
		for _, v := range ips {
			_, _ = i.GetDc(v.IP)
		}
	}
	b.ReportMetric(float64(len(ips)), "len")
	b.ReportAllocs()

	i.Destroy()
}

func BenchmarkIpDcGetMany(b *testing.B) {
	verboseLevel := 0

	i := NewIPDCWithDefaults(verboseLevel)
	i.netListFile = "networklist-perdc.txt.gz"
	//i.netListData = data
	_, _ = i.GetDc(net.ParseIP("1.1.1.1"))

	ipStrs := []string{
		"2a02:6b8:c00:388e:0:4c7d:e675:7d", "2a02:6b8:c00:381c:0:4c7d:46ec:de63", "2a02:6b8:c00:318e:0:4c7d:e9f0:3933", "2a02:6b8:c16:2915:0:4c7d:1eaf:593f",
		"2a02:6b8:c16:2797:0:4c7d:816f:bfb9", "2a02:6b8:c16:2705:0:4c7d:ffcb:fa1b", "2a02:6b8:c18:422:0:4c7d:9da0:1d12", "2a02:6b8:c18:6a0:0:4c7d:9ae9:205a",
		"2a02:6b8:c18:a8a:0:4c7d:691c:b953", "2a02:6b8:c13:f9c:0:4c7d:9bac:6769", "2a02:6b8:c13:602:0:4c7d:49f4:ad41", "2a02:6b8:c13:912:0:4c7d:7102:cf65",
		"2a02:6b8:c00:388e:0:4c7d:daed:3334", "2a02:6b8:c00:381c:0:4c7d:c573:2e30", "2a02:6b8:c00:318e:0:4c7d:e89f:e45b", "2a02:6b8:c16:2915:0:4c7d:340c:83fc",
		"2a02:6b8:c16:2797:0:4c7d:1ef1:e42a", "2a02:6b8:c16:2705:0:4c7d:d09a:1f44", "2a02:6b8:c18:422:0:4c7d:532b:6d6a", "2a02:6b8:c18:6a0:0:4c7d:e52e:27ed",
		"2a02:6b8:c18:a8a:0:4c7d:3f3c:dc70", "2a02:6b8:c13:f9c:0:4c7d:6829:8945", "2a02:6b8:c13:602:0:4c7d:a573:6aaa", "2a02:6b8:c13:912:0:4c7d:d7d0:7ee3",
		"2a02:6b8:c16:2797:0:4c7d:ec41:176f", "2a02:6b8:c16:2705:0:4c7d:6d19:2038", "2a02:6b8:c18:6a0:0:4c7d:7513:4dba", "2a02:6b8:c18:a8a:0:4c7d:239c:c110",
	}

	ipsMany := []net.IP{}
	for _, v := range ipStrs {
		ipsMany = append(ipsMany, net.ParseIP(v))
	}

	b.ResetTimer()
	for ix := 0; ix < b.N; ix++ {
		_, _ = i.GetDcMany(ipsMany)
	}
	b.ReportMetric(float64(len(ipsMany)), "len")
	b.ReportAllocs()

	i.Destroy()
}
