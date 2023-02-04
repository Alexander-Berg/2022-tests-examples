package parser

import (
	"bytes"
	"testing"
	"time"
)

func TestEmptyString(t *testing.T) {
	err, succ := DataMatcher([]byte(""))

	if len(err) != 0 || len(succ) != 0 {
		t.Errorf("Empty string returned [err: %d, succ: %d]. Should be empty", len(err), len(succ))
	}

	err, succ = DataMatcher([]byte("\n"))

	if len(err) != 0 || len(succ) != 0 {
		t.Errorf("Empty string returned [err: %d, succ: %d]. Should be empty", len(err), len(succ))
	}
}

const (
	cacheHitURL = "/1.js"
	ok200URL    = "/2.js"
	ok404URL    = "/3.js"

	cacheHitStatus = 200
	ok200Status    = 200
	ok404Status    = 404
)

var timeZone = time.FixedZone("MSK", 3*60*60)

var (
	cacheHitDate = time.Date(2021, time.February, 23, 18, 15, 30, 2089, timeZone)
	ok200Date    = time.Date(2021, time.February, 23, 18, 15, 30, 2089, timeZone)
	ok404Date    = time.Date(2021, time.February, 23, 18, 15, 30, 2089, timeZone)
)

var (
	cacheHitRecord = ParserRecord{Date: cacheHitDate, URL: cacheHitURL, Code: cacheHitStatus}
	ok200Record    = ParserRecord{Date: ok200Date, URL: ok200URL, Code: ok200Status}
	ok404Record    = ParserRecord{Date: ok404Date, URL: ok404URL, Code: ok404Status}
)

var (
	cacheHitRequest = []byte(
		"ip_port=127.0.0.1:31337\t" +
			"timestamp=2021-02-23T18:15:30.002089+0300\t" +
			"query=\"GET /1.js HTTP/1.1\"\t" +
			"work_time=0.000580s\t" +
			"referer=\"\"\t" +
			"host=\"yastatic.net\"\t" +
			"workflow= [report u:service_total [regexp default [regexp default [report u:total_request_with_nel [log_headers <::accept-encoding:gzip, deflate, br::> " +
			"<::user-agent:Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.150 Safari/537.36 OPR/74.0.3911.107 (Edition Yx)::>" +
			" <::cookie:_ym_d=1609503574; _ym_uid=16095035741057288650::> [regexp from_cdn [regexp main [regexp_path yastatic_net_s3_video-player" +
			" [report u:yastatic_net_s3_video-player [regexp br_section [report u:s3_cached_report [log_headers <::Host:video-player-static.s3.yandex.net::>" +
			" [report u:total_request_with_macache [regexp default [log_headers /1.js cache hit 2021-02-26T16:57:02.174352Z]]]]]]]]]] <::Etag:\"x\"::>]]]]]\n")

	ok200Request = []byte(
		"ip_port=127.0.0.1:31337\t" +
			"timestamp=2021-02-23T18:15:30.002089+0300\t" +
			"query=\"GET /2.js HTTP/1.1\"\t" +
			"work_time=0.000580s\t" +
			"referer=\"\"\t" +
			"host=\"yastatic.net\"\t" +
			"workflow= [report u:service_total [regexp default [regexp default [report u:total_request_with_nel [log_headers <::accept-encoding:gzip, deflate, br::> " +
			"<::user-agent:Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.150 Safari/537.36 OPR/74.0.3911.107 (Edition Yx)::>" +
			" <::cookie:_ym_d=1609503574; _ym_uid=16095035741057288650::> [regexp from_cdn [regexp main [regexp_path yastatic_net_s3_video-player" +
			" [report u:yastatic_net_s3_video-player [regexp br_section [report u:s3_cached_report [log_headers <::Host:video-player-static.s3.yandex.net::>" +
			" [report u:total_request_with_macache [regexp default [log_headers /2.js succ 200]]]]]]]]]] <::Etag:\"x\"::>]]]]]\n")

	ok404Request = []byte(
		"ip_port=127.0.0.1:31337\t" +
			"timestamp=2021-02-23T18:15:30.002089+0300\t" +
			"query=\"GET /3.js HTTP/1.1\"\t" +
			"work_time=0.000580s\t" +
			"referer=\"\"\t" +
			"host=\"yastatic.net\"\t" +
			"workflow= [report u:service_total [regexp default [regexp default [report u:total_request_with_nel [log_headers <::accept-encoding:gzip, deflate, br::> " +
			"<::user-agent:Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.150 Safari/537.36 OPR/74.0.3911.107 (Edition Yx)::>" +
			" <::cookie:_ym_d=1609503574; _ym_uid=16095035741057288650::> [regexp from_cdn [regexp main [regexp_path yastatic_net_s3_video-player" +
			" [report u:yastatic_net_s3_video-player [regexp br_section [report u:s3_cached_report [log_headers <::Host:video-player-static.s3.yandex.net::>" +
			" [report u:total_request_with_macache [regexp default [log_headers /3.js succ 404]]]]]]]]]] <::Etag:\"x\"::>]]]]]\n")
)

func compareRecords(t *testing.T, testData ParserRecord, parserData ParserRecord) {
	if testData.Date.Equal(parserData.Date) {
		t.Errorf("got %s date. Should be %s", parserData.Date, testData.Date)
	}

	if testData.URL != parserData.URL {
		t.Errorf("got %s URL. Should be %s", parserData.URL, testData.URL)
	}

	if testData.Code != parserData.Code {
		t.Errorf("got %d status. Should be %d", parserData.Code, testData.Code)
	}
}

func TestValidSingleRecord(t *testing.T) {
	_, succ := DataMatcher(cacheHitRequest)
	if len(succ) != 1 {
		t.Errorf("returned [succ: %d]. Should be 1", len(succ))
	}
	compareRecords(t, cacheHitRecord, succ[0])
}

func TestValidMultiRecord(t *testing.T) {
	err, succ := DataMatcher(bytes.Join([][]byte{cacheHitRequest, ok200Request, ok404Request}, nil))
	if len(succ) != 2 && len(err) != 1 {
		t.Errorf("returned [succ: %d, err: %d]. Should be not empty", len(succ), len(err))
	}
	compareRecords(t, cacheHitRecord, succ[0])
	compareRecords(t, ok200Record, succ[1])
	compareRecords(t, ok404Record, err[0])
}

func TestTabBeforeEol(t *testing.T) {
	broken := []byte("ip_port=127.0.0.1:31337\t" +
		"timestamp=2021-02-23T18:15:30.002089+0300\t" +
		"query=\"GET /1.js HTTP/1.0\"\t" +
		"workflow= cache hit\t\n")
	_, succ := DataMatcher(broken)
	if len(succ) != 1 {
		t.Errorf("tab before eol returned [succ: %d]. Should be 1", len(succ))
	}
	compareRecords(t, cacheHitRecord, succ[0])
}

func TestTextBeforeIpProto(t *testing.T) {
	_, succ := DataMatcher(bytes.Join([][]byte{[]byte("xx\t"), cacheHitRequest}, nil))
	if len(succ) != 1 {
		t.Errorf("tab string returned [succ: %d]. Should be 1", len(succ))
	}
	compareRecords(t, cacheHitRecord, succ[0])

	_, succ = DataMatcher(bytes.Join([][]byte{[]byte("xx\n"), cacheHitRequest}, nil))
	if len(succ) != 1 {
		t.Errorf("eol string returned [succ: %d]. Should be 1", len(succ))
	}
	compareRecords(t, cacheHitRecord, succ[0])

	/* should be passed because right now we don't proceed ip_proto field */
	_, succ = DataMatcher(bytes.Join([][]byte{[]byte("xx"), cacheHitRequest}, nil))
	if len(succ) != 1 {
		t.Errorf("simple string returned [succ: %d]. Should be 1", len(succ))
	}
	compareRecords(t, cacheHitRecord, succ[0])

	_, succ = DataMatcher(bytes.Join([][]byte{[]byte("\n"), cacheHitRequest}, nil))
	if len(succ) != 1 {
		t.Errorf("simple string returned [succ: %d]. Should be 1", len(succ))
	}
	compareRecords(t, cacheHitRecord, succ[0])
}

func TestBrokenTimestamp(t *testing.T) {
	broken := []byte("ip_port=127.0.0.1:31337\t" +
		"timestamp=xxx\t" +
		"query=\"GET / HTTP/1.0\"\t" +
		"workflow= cache hit\n")
	_, succ := DataMatcher(bytes.Join([][]byte{broken, cacheHitRequest}, nil))
	if len(succ) != 1 {
		t.Errorf("incorrect timestamp returned [succ: %d]. Should be 1", len(succ))
	}
	compareRecords(t, cacheHitRecord, succ[0])
}

func TestBrokenQuery(t *testing.T) {
	broken := []byte("ip_port=127.0.0.1:31337\t" +
		"timestamp=2021-02-23T18:15:30.002089+0300\t" +
		"query=1\t" +
		"workflow= cache hit\n")
	_, succ := DataMatcher(bytes.Join([][]byte{broken, cacheHitRequest}, nil))
	if len(succ) != 1 {
		t.Errorf("query string \"1\" returned [succ: %d]. Should be 1", len(succ))
	}
	compareRecords(t, cacheHitRecord, succ[0])

	broken = []byte("ip_port=127.0.0.1:31337\t" +
		"timestamp=2021-02-23T18:15:30.002089+0300\t" +
		"query=\"GET\t" +
		"workflow= cache hit\n")
	_, succ = DataMatcher(bytes.Join([][]byte{broken, cacheHitRequest}, nil))
	if len(succ) != 1 {
		t.Errorf("query string \"GET\" returned [succ: %d]. Should be 1", len(succ))
	}
	compareRecords(t, cacheHitRecord, succ[0])

	broken = []byte("ip_port=127.0.0.1:31337\t" +
		"timestamp=2021-02-23T18:15:30.002089+0300\t" +
		"query=\"GET \t" +
		"workflow= cache hit\n")
	_, succ = DataMatcher(bytes.Join([][]byte{broken, cacheHitRequest}, nil))
	if len(succ) != 1 {
		t.Errorf("query string \"GET \" returned [succ: %d]. Should be 1", len(succ))
	}
	compareRecords(t, cacheHitRecord, succ[0])

	broken = []byte("ip_port=127.0.0.1:31337\t" +
		"timestamp=2021-02-23T18:15:30.002089+0300\t" +
		"query=\"GET /\t" +
		"workflow= cache hit\n")
	_, succ = DataMatcher(bytes.Join([][]byte{broken, cacheHitRequest}, nil))
	if len(succ) != 1 {
		t.Errorf("query string \"GET /\" returned [succ: %d]. Should be 1", len(succ))
	}
	compareRecords(t, cacheHitRecord, succ[0])

	broken = []byte("ip_port=127.0.0.1:31337\t" +
		"timestamp=2021-02-23T18:15:30.002089+0300\t" +
		"query=\"GET / \t" +
		"workflow= cache hit\n")
	_, succ = DataMatcher(bytes.Join([][]byte{broken, cacheHitRequest}, nil))
	if len(succ) != 1 {
		t.Errorf("query string \"GET / \" returned [succ: %d]. Should be 1", len(succ))
	}
	compareRecords(t, cacheHitRecord, succ[0])

	broken = []byte("ip_port=127.0.0.1:31337\t" +
		"timestamp=2021-02-23T18:15:30.002089+0300\t" +
		"query=\"GET +.,/2.js HTTP/1.0\t" +
		"workflow= cache hit\n")
	_, succ = DataMatcher(bytes.Join([][]byte{broken, cacheHitRequest}, nil))
	if len(succ) != 1 {
		t.Errorf("query string \"+.,/2.js\" returned [succ: %d]. Should be 1", len(succ))
	}
	compareRecords(t, cacheHitRecord, succ[0])
}

func TestBrokenWorkflow(t *testing.T) {
	broken := []byte("ip_port=127.0.0.1:31337\t" +
		"timestamp=2021-02-23T18:15:30.002089+0300\t" +
		"query=\"GET / HTTP/1.0\"\t" +
		"workflow= cache miss\n")
	_, succ := DataMatcher(broken)
	if len(succ) != 0 {
		t.Errorf("cache miss workflow returned [succ: %d]. Should be 1", len(succ))
	}

	broken = []byte("ip_port=127.0.0.1:31337\t" +
		"timestamp=2021-02-23T18:15:30.002089+0300\t" +
		"query=\"GET /1.js HTTP/1.0\"\t" +
		"workflow= [log_headers /1.js cache hit 2021-02-26T16:57:02.174352Z] [log_headers /2.js succ 404]\n")
	_, succ = DataMatcher(broken)
	if len(succ) != 1 {
		t.Errorf("cache miss workflow returned [succ: %d]. Should be 1", len(succ))
	}
	compareRecords(t, cacheHitRecord, succ[0])

	broken = []byte("ip_port=127.0.0.1:31337\t" +
		"timestamp=2021-02-23T18:15:30.002089+0300\t" +
		"query=\"GET /1.js HTTP/1.0\"\t" +
		"workflow= [log_headers /1.js succ 404] [log_headers /1.js succ 200]\n")
	_, succ = DataMatcher(broken)
	if len(succ) != 1 {
		t.Errorf("cache miss workflow returned [succ: %d]. Should be 1", len(succ))
	}
	compareRecords(t, cacheHitRecord, succ[0])
}

func TestEmptyFields(t *testing.T) {
	broken := []byte("ip_port=127.0.0.1:31337\t" +
		"query=\"GET / HTTP/1.0\"\t" +
		"workflow= cache hit\n")
	_, succ := DataMatcher(bytes.Join([][]byte{broken, cacheHitRequest}, nil))
	if len(succ) != 1 {
		t.Errorf("non-existing timestamp returned [succ: %d]. Should be 1", len(succ))
	}
	compareRecords(t, cacheHitRecord, succ[0])

	broken = []byte("ip_port=127.0.0.1:31337\t" +
		"timestamp=2021-02-23T18:15:30.002089+0300\t" +
		"workflow= cache hit\n")
	_, succ = DataMatcher(bytes.Join([][]byte{broken, cacheHitRequest}, nil))
	if len(succ) != 1 {
		t.Errorf("non-existing query returned [succ: %d]. Should be 1", len(succ))
	}
	compareRecords(t, cacheHitRecord, succ[0])

	broken = []byte("ip_port=127.0.0.1:31337\t" +
		"timestamp=2021-02-23T18:15:30.002089+0300\t" +
		"query=\"GET / HTTP/1.0\"\n")
	_, succ = DataMatcher(bytes.Join([][]byte{broken, cacheHitRequest}, nil))
	if len(succ) != 1 {
		t.Errorf("non-existing workflow returned [succ: %d]. Should be 1", len(succ))
	}
	compareRecords(t, cacheHitRecord, succ[0])
}

func TestEolBeforeKVDelim(t *testing.T) {
	broken := []byte("ip_port=127.0.0.1:31337\t" +
		"timestamp\n=2021-02-23T18:15:30.002089+0300\t" +
		"query=\"GET /test HTTP/1.0\t" +
		"workflow= cache hit\n")
	_, succ := DataMatcher(bytes.Join([][]byte{broken, cacheHitRequest}, nil))
	if len(succ) != 1 {
		t.Errorf("eol in timestamp before \"=\" returned [succ: %d]. Should be 1", len(succ))
	}
	compareRecords(t, cacheHitRecord, succ[0])

	broken = []byte("ip_port=127.0.0.1:31337\t" +
		"timestamp=2021-02-23T18:15:30.002089+0300\t" +
		"query\n=\"GET /test HTTP/1.0\t" +
		"workflow= cache hit\n")
	_, succ = DataMatcher(bytes.Join([][]byte{broken, cacheHitRequest}, nil))
	if len(succ) != 1 {
		t.Errorf("eol in query before \"=\" returned [succ: %d]. Should be 1", len(succ))
	}
	compareRecords(t, cacheHitRecord, succ[0])

	broken = []byte("ip_port=127.0.0.1:31337\t" +
		"timestamp=2021-02-23T18:15:30.002089+0300\t" +
		"query=\"GET /test HTTP/1.0\t" +
		"workflow\n= cache hit\n")
	_, succ = DataMatcher(bytes.Join([][]byte{broken, cacheHitRequest}, nil))
	if len(succ) != 1 {
		t.Errorf("eol in workflow before \"=\" returned [succ: %d]. Should be 1", len(succ))
	}
	compareRecords(t, cacheHitRecord, succ[0])
}

func TestEolAfterKVDelim(t *testing.T) {
	broken := []byte("ip_port=127.0.0.1:31337\t" +
		"timestamp=\n2021-02-23T18:15:30.002089+0300\t" +
		"query=\"GET /test HTTP/1.0\t" +
		"workflow= cache hit\n")
	_, succ := DataMatcher(bytes.Join([][]byte{broken, cacheHitRequest}, nil))
	if len(succ) != 1 {
		t.Errorf("eol in timestamp after \"=\" returned [succ: %d]. Should be 1", len(succ))
	}
	compareRecords(t, cacheHitRecord, succ[0])

	broken = []byte("ip_port=127.0.0.1:31337\t" +
		"timestamp=2021-02-23T18:15:30.002089+0300\t" +
		"query=\n\"GET /test HTTP/1.0\t" +
		"workflow= cache hit\n")
	_, succ = DataMatcher(bytes.Join([][]byte{broken, cacheHitRequest}, nil))
	if len(succ) != 1 {
		t.Errorf("eol in query after \"=\" returned [succ: %d]. Should be 1", len(succ))
	}
	compareRecords(t, cacheHitRecord, succ[0])

	broken = []byte("ip_port=127.0.0.1:31337\t" +
		"timestamp=2021-02-23T18:15:30.002089+0300\t" +
		"query=\"GET /test HTTP/1.0\t" +
		"workflow=\n cache hit\n")
	_, succ = DataMatcher(bytes.Join([][]byte{broken, cacheHitRequest}, nil))
	if len(succ) != 1 {
		t.Errorf("eol in workflow after \"=\" returned [succ: %d]. Should be 1", len(succ))
	}
	compareRecords(t, cacheHitRecord, succ[0])
}

func TestEolInsteadKVDelim(t *testing.T) {
	broken := []byte("ip_port=127.0.0.1:31337\t" +
		"timestamp\n2021-02-23T18:15:30.002089+0300\t" +
		"query=\"GET /test HTTP/1.0\t" +
		"workflow= cache hit\n")
	_, succ := DataMatcher(bytes.Join([][]byte{broken, cacheHitRequest}, nil))
	if len(succ) != 1 {
		t.Errorf("eol in timestamp instead \"=\" returned [succ: %d]. Should be 1", len(succ))
	}
	compareRecords(t, cacheHitRecord, succ[0])

	broken = []byte("ip_port=127.0.0.1:31337\t" +
		"timestamp=2021-02-23T18:15:30.002089+0300\t" +
		"query\n\"GET /test HTTP/1.0\t" +
		"workflow= cache hit\n")
	_, succ = DataMatcher(bytes.Join([][]byte{broken, cacheHitRequest}, nil))
	if len(succ) != 1 {
		t.Errorf("eol in query instead \"=\" returned [succ: %d]. Should be 1", len(succ))
	}
	compareRecords(t, cacheHitRecord, succ[0])

	broken = []byte("ip_port=127.0.0.1:31337\t" +
		"timestamp=2021-02-23T18:15:30.002089+0300\t" +
		"query=\"GET /test HTTP/1.0\t" +
		"workflow\n cache hit\n")
	_, succ = DataMatcher(bytes.Join([][]byte{broken, cacheHitRequest}, nil))
	if len(succ) != 1 {
		t.Errorf("eol in workflow instead \"=\" returned [succ: %d]. Should be 1", len(succ))
	}
	compareRecords(t, cacheHitRecord, succ[0])
}

func TestTabBeforeKVDelim(t *testing.T) {
	broken := []byte("ip_port=127.0.0.1:31337\t" +
		"timestamp\t=2021-02-23T18:15:30.002089+0300\t" +
		"query=\"GET /test HTTP/1.0\t" +
		"workflow= cache hit\n")
	_, succ := DataMatcher(bytes.Join([][]byte{broken, cacheHitRequest}, nil))
	if len(succ) != 1 {
		t.Errorf("tab in timestamp before \"=\" returned [succ: %d]. Should be 1", len(succ))
	}
	compareRecords(t, cacheHitRecord, succ[0])

	broken = []byte("ip_port=127.0.0.1:31337\t" +
		"timestamp=2021-02-23T18:15:30.002089+0300\t" +
		"query\t=\"GET /test HTTP/1.0\t" +
		"workflow= cache hit\n")
	_, succ = DataMatcher(bytes.Join([][]byte{broken, cacheHitRequest}, nil))
	if len(succ) != 1 {
		t.Errorf("tab in query before \"=\" returned [succ: %d]. Should be 1", len(succ))
	}
	compareRecords(t, cacheHitRecord, succ[0])

	broken = []byte("ip_port=127.0.0.1:31337\t" +
		"timestamp=2021-02-23T18:15:30.002089+0300\t" +
		"query=\"GET /test HTTP/1.0\t" +
		"workflow\t= cache hit\n")
	_, succ = DataMatcher(bytes.Join([][]byte{broken, cacheHitRequest}, nil))
	if len(succ) != 1 {
		t.Errorf("tab in workflow before \"=\" returned [succ: %d]. Should be 1", len(succ))
	}
	compareRecords(t, cacheHitRecord, succ[0])
}

func TestTabAfterKVDelim(t *testing.T) {
	broken := []byte("ip_port=127.0.0.1:31337\t" +
		"timestamp=\t2021-02-23T18:15:30.002089+0300\t" +
		"query=\"GET /test HTTP/1.0\t" +
		"workflow= cache hit\n")
	_, succ := DataMatcher(bytes.Join([][]byte{broken, cacheHitRequest}, nil))
	if len(succ) != 1 {
		t.Errorf("tab in timestamp after \"=\" returned [succ: %d]. Should be 1", len(succ))
	}
	compareRecords(t, cacheHitRecord, succ[0])

	broken = []byte("ip_port=127.0.0.1:31337\t" +
		"timestamp=2021-02-23T18:15:30.002089+0300\t" +
		"query=\t\"GET /test HTTP/1.0\t" +
		"workflow= cache hit\n")
	_, succ = DataMatcher(bytes.Join([][]byte{broken, cacheHitRequest}, nil))
	if len(succ) != 1 {
		t.Errorf("tab in query after \"=\" returned [succ: %d]. Should be 1", len(succ))
	}
	compareRecords(t, cacheHitRecord, succ[0])

	broken = []byte("ip_port=127.0.0.1:31337\t" +
		"timestamp=2021-02-23T18:15:30.002089+0300\t" +
		"query=\"GET /test HTTP/1.0\t" +
		"workflow=\t cache hit\n")
	_, succ = DataMatcher(bytes.Join([][]byte{broken, cacheHitRequest}, nil))
	if len(succ) != 1 {
		t.Errorf("tab in workflow after \"=\" returned [succ: %d]. Should be 1", len(succ))
	}
	compareRecords(t, cacheHitRecord, succ[0])
}

func TestTabInsteadKVDelim(t *testing.T) {
	broken := []byte("ip_port=127.0.0.1:31337\t" +
		"timestamp\t2021-02-23T18:15:30.002089+0300\t" +
		"query=\"GET /test HTTP/1.0\t" +
		"workflow= cache hit\n")
	_, succ := DataMatcher(bytes.Join([][]byte{broken, cacheHitRequest}, nil))
	if len(succ) != 1 {
		t.Errorf("tab in timestamp instead \"=\" returned [succ: %d]. Should be 1", len(succ))
	}
	compareRecords(t, cacheHitRecord, succ[0])

	broken = []byte("ip_port=127.0.0.1:31337\t" +
		"timestamp=2021-02-23T18:15:30.002089+0300\t" +
		"query\t\"GET /test HTTP/1.0\t" +
		"workflow= cache hit\n")
	_, succ = DataMatcher(bytes.Join([][]byte{broken, cacheHitRequest}, nil))
	if len(succ) != 1 {
		t.Errorf("tab in query instead \"=\" returned [succ: %d]. Should be 1", len(succ))
	}
	compareRecords(t, cacheHitRecord, succ[0])

	broken = []byte("ip_port=127.0.0.1:31337\t" +
		"timestamp=2021-02-23T18:15:30.002089+0300\t" +
		"query=\"GET /test HTTP/1.0\t" +
		"workflow\t cache hit\n")
	_, succ = DataMatcher(bytes.Join([][]byte{broken, cacheHitRequest}, nil))
	if len(succ) != 1 {
		t.Errorf("tab in workflow instead \"=\" returned [succ: %d]. Should be 1", len(succ))
	}
	compareRecords(t, cacheHitRecord, succ[0])
}
