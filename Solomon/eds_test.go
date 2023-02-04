package eds

import (
	"fmt"
	"testing"
	"time"
)

func TestEds(t *testing.T) {
	goodCacheTime := time.Second
	badCacheTime := time.Second
	prefetchTime := 5 * time.Second
	cleanUpInterval := 15 * time.Second
	requestTimeout := 5 * time.Second
	cacheSize := 50
	workers := 10
	serveStale := true
	verboseLevel := 2

	c, err := NewEdsCache(goodCacheTime, badCacheTime, prefetchTime, cleanUpInterval, requestTimeout,
		cacheSize, workers,
		serveStale,
		verboseLevel)
	if err != nil {
		t.Errorf("failed to create eds cache, %v", err)
	}
	resps := c.Get([]*EdsRequest{{
		Pattern:  "alerting-*.mon.cloud-preprod.yandex.net",
		Endpoint: "xds.dns.cloud-preprod.yandex.net:18000",
	}},
		nil,
	)
	for _, resp := range resps {
		fmt.Printf("%#v err=%v\n", resp.Hosts, resp.Error)
	}
}
