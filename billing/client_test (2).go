package interactions

import (
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	"gopkg.in/yaml.v2"
)

func TestMakeURLWithoutTrailingSlash(t *testing.T) {
	client := Client{
		config: Config{
			Debug:   false,
			BaseURL: "https://mydomain.com/level1/level2",
		},
	}
	url, err := client.MakeURL("/some_path")
	expected := "https://mydomain.com/level1/level2/some_path"
	if err != nil || url != expected {
		t.Errorf("FAILED: expecting url to be \"%v\" but actual was \"%v\" ", expected, url)
	}
}

func TestMakeURLError(t *testing.T) {
	client := Client{
		config: Config{
			Debug:   false,
			BaseURL: "123123###%",
		},
	}
	url, err := client.MakeURL("/some_path")
	if err == nil {
		t.Errorf("FAILED: expecting error but actual was \"%v\" ", url)
	}
}

func TestMakeURLWithTrailingSlash(t *testing.T) {
	client := Client{
		config: Config{
			Debug:   false,
			BaseURL: "https://mydomain.com/level1/level2",
		},
	}
	url, err := client.MakeURL("/some_path/")
	expected := "https://mydomain.com/level1/level2/some_path/"
	if err != nil || url != expected {
		t.Errorf("FAILED: expecting url to be \"%v\" but actual was \"%v\" ", expected, url)
	}
}

func BenchmarkMakeURLWithTrailingSlash(b *testing.B) {
	for i := 0; i < b.N; i++ {
		client := Client{
			config: Config{
				Debug:   false,
				BaseURL: "https://mydomain.com/level1/level2",
			},
		}
		url, err := client.MakeURL("/some_path/")
		expected := "https://mydomain.com/level1/level2/some_path/"
		if err != nil || url != expected {
			b.Errorf("FAILED: expecting url to be \"%v\" but actual was \"%v\" ", expected, url)
		}
	}
}

func TestMakeURLWithoutPath(t *testing.T) {
	client := Client{
		config: Config{
			Debug:   false,
			BaseURL: "https://mydomain.com/level1/level2",
		},
	}
	url, err := client.MakeURL("")
	expected := "https://mydomain.com/level1/level2"
	if err != nil || url != expected {
		t.Errorf("FAILED: expecting url to be \"%v\" but actual was \"%v\" ", expected, url)
	}
}

func TestMakeURLWithOnlyBaseUrl(t *testing.T) {
	client := Client{
		config: Config{
			Debug:   false,
			BaseURL: "https://mydomain.com",
		},
	}
	url, err := client.MakeURL("")
	expected := "https://mydomain.com"
	if err != nil || url != expected {
		t.Errorf("FAILED: expecting url to be \"%v\" but actual was \"%v\" ", expected, url)
	}
}

func TestMakeURLWithoutProtocol(t *testing.T) {
	client := Client{
		config: Config{
			Debug:   false,
			BaseURL: "mydomain.com",
		},
	}
	url, err := client.MakeURL("")
	expected := "mydomain.com"
	if err != nil || url != expected {
		t.Errorf("FAILED: expecting url to be \"%v\" but actual was \"%v\" ", expected, url)
	}
}

func TestMakeURLWithProtocolAndPort(t *testing.T) {
	client := Client{
		config: Config{
			Debug:   false,
			BaseURL: "mydomain.com:8080/path1/path2",
		},
	}
	url, err := client.MakeURL("")
	expected := "mydomain.com:8080/path1/path2"
	if err != nil || url != expected {
		t.Errorf("FAILED: expecting url to be \"%v\" but actual was \"%v\" ", expected, url)
	}
}

func TestMakeURLWithProtocolAndPortAndLocalPart(t *testing.T) {
	client := Client{
		config: Config{
			Debug:   false,
			BaseURL: "mydomain.com:8080/path1/path2",
		},
	}
	url, err := client.MakeURL("/path3")
	expected := "mydomain.com:8080/path1/path2/path3"
	if err != nil || url != expected {
		t.Errorf("FAILED: expecting url to be \"%v\" but actual was \"%v\" ", expected, url)
	}
}

func TestMakeURLWithProtocolAndPortAndLocalPart2(t *testing.T) {
	client := Client{
		config: Config{
			Debug:   false,
			BaseURL: "mydomain.com:8080/path1/path2",
		},
	}
	url, err := client.MakeURL("/path3/")
	expected := "mydomain.com:8080/path1/path2/path3/"
	if err != nil || url != expected {
		t.Errorf("FAILED: expecting url to be \"%v\" but actual was \"%v\" ", expected, url)
	}
}

func TestMakeURLWithoutInitialSlashInURLPath(t *testing.T) {
	client := Client{
		config: Config{
			Debug:   false,
			BaseURL: "mydomain.com:8080/path1/path2",
		},
	}
	url, err := client.MakeURL("path3/")
	expected := "mydomain.com:8080/path1/path2/path3/"
	if err != nil || url != expected {
		t.Errorf("FAILED: expecting url to be \"%v\" but actual was \"%v\" ", expected, url)
	}
}

type TestTimeoutConfig struct {
	Timeout TimeoutType `yaml:"timeout,omitempty"`
}

func TestTimeoutTypeEmptyStringMarshal(t *testing.T) {
	var testConf TestTimeoutConfig
	err := yaml.Unmarshal([]byte(""), &testConf)
	assert.NoError(t, err)
	assert.Equal(t, testConf.Timeout, TimeoutType(0))
}

func TestTimeoutTypeWithoutDimensionUnmarshal(t *testing.T) {
	var testConf TestTimeoutConfig
	err := yaml.Unmarshal([]byte("timeout: 10"), &testConf)
	assert.ErrorContains(t, err, "timeout: 10 can't be just number, please specify dimension i.e. 1s, 100ms")
	assert.Equal(t, TimeoutType(0), testConf.Timeout)
}

func TestTimeoutTypeMsUnmarshal(t *testing.T) {
	var testConf TestTimeoutConfig
	err := yaml.Unmarshal([]byte("timeout: 10ms"), &testConf)
	assert.NoError(t, err)
	assert.Equal(t, TimeoutType(10*time.Millisecond), testConf.Timeout)
}

func TestTimeoutTypeUnmarshalMarshal(t *testing.T) {
	expectedConf := TestTimeoutConfig{
		Timeout: TimeoutType(117900),
	}
	var testConf TestTimeoutConfig
	str, errMarshal := yaml.Marshal(expectedConf)
	assert.NoError(t, errMarshal)
	errUnmarshal := yaml.Unmarshal(str, &testConf)
	assert.NoError(t, errUnmarshal)
	assert.Equal(t, expectedConf, testConf)
}

// TODO more tests on invalid urls to make sure MakeURL returns error
