package config

import (
	"os"
	"reflect"
	"testing"
	"time"
)

func TestParseFromYaml(t *testing.T) {
	expected := &Settings{
		Host:              "prometheus",
		PrometheusAddress: "foo",
		JugglerAddress:    "bar",
		SyncPeriod:        1 * time.Minute,
		ListenAddress:     ":1234",
	}
	source := []byte(`
host: prometheus
prometheus-address: foo
juggler-address: bar
sync-period: 1m
listen-address: ":1234"`)
	actual := parseFromYaml(source)
	if !reflect.DeepEqual(actual, expected) {
		t.Errorf("parseFromYaml(%s) = %s, want %s", source, actual, expected)
	}

}

func TestCreateConfigPanicIfNoJugglerAddress(t *testing.T) {
	defer func() {
		if r := recover(); r == nil {
			t.Errorf("The code did not panic")
		}
	}()

	os.Args = []string{"cmd", "-juggler-address=bla", "-prometheus-address=var"}
	Discover()
}
