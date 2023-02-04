package cache

import (
	"encoding/gob"
	"github.com/YandexClassifieds/envoy-api/utils"
	"github.com/stretchr/testify/assert"
	"io/ioutil"
	"os"
	"testing"
)

type ServiceMap map[string]*ServiceInfo

type ServiceInfo struct {
	Name          string
	Type          string
	HttpDomains   utils.StringSet
	TcpListenPort int
}

func TestLoadCache(t *testing.T) {
	f, err := ioutil.TempFile(os.TempDir(), "apitest")
	defer f.Close()
	defer os.Remove(f.Name())
	if err != nil {
		t.Errorf("tmp file create fail: %v", err)
		return
	}
	inObj := ServiceMap{
		"a": &ServiceInfo{
			Name: "my-service",
		},
	}
	enc := gob.NewEncoder(f)
	err = enc.Encode(inObj)
	if err != nil {
		t.Errorf("failed to write dummyType: %v", err)
		return
	}

	outObj := make(ServiceMap)
	os.TempDir()
	err = LoadCache(f.Name(), &outObj)
	if !assert.NoError(t, err, "load cache failed") {
		return
	}
	assert.Equal(t, inObj, outObj)
}
