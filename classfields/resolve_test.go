package resolve

import (
	"errors"
	"github.com/stretchr/testify/assert"
	"net"
	"reflect"
	"testing"
)

func checkGetDatacenter(t *testing.T, address string, expectedDatacenter string, expectedErr error) {
	t.Helper()
	datacenter, err := GetDatacenter(address)
	assert.Equal(t, expectedErr, err, "err check fail")
	assert.Equal(t, expectedDatacenter, datacenter)
}

func TestGetDatacenter(t *testing.T) {
	checkGetDatacenter(t, "tcp://shard-01-sas.test.vertis.yandex.net:36310", "sas", nil)
	checkGetDatacenter(t, "shard-01-myt.test.vertis.yandex.net:34337", "myt", nil)
	checkGetDatacenter(t, "shard-01-iv.test.vertis.yandex.net:36310", "",
		errors.New("failed to identify host datacenter: hostname 'shard-01-iv.test.vertis.yandex.net:36310' doesn't contain 'sas', 'myt', 'vla' or 'man'"))
	checkGetDatacenter(t, "shard-01-my.test.vertis.yandex.net", "",
		errors.New("failed to identify host datacenter: hostname 'shard-01-my.test.vertis.yandex.net' doesn't contain 'sas', 'myt', 'vla' or 'man'"))
	checkGetDatacenter(t, "shard-01-vl.test.vertis.yandex.net", "",
		errors.New("failed to identify host datacenter: hostname 'shard-01-vl.test.vertis.yandex.net' doesn't contain 'sas', 'myt', 'vla' or 'man'"))
}

func checkGetHostAndPort(t *testing.T, address string, expectedHost string, expectedPort int64, expectedErr error) {
	t.Helper()
	host, port, err := GetHostAndPort(address)
	assert.Equal(t, expectedErr, err)
	assert.Equal(t, expectedHost, host)
	assert.Equal(t, expectedPort, port)
}

func TestGetHostAndPort(t *testing.T) {
	checkGetHostAndPort(t, "shard-01-myt.test.vertis.yandex.net:34337", "shard-01-myt.test.vertis.yandex.net", 34337, nil)
	checkGetHostAndPort(t, "tcp://shard-01-sas.test.vertis.yandex.net:36310", "shard-01-sas.test.vertis.yandex.net", 36310, nil)
	checkGetHostAndPort(t, "tcp://shard-01-sas.test.vertis.yandex.net36310", "", 0,
		errors.New("invalid address 'shard-01-sas.test.vertis.yandex.net36310': address and port should be separated by semicolon"))
	checkGetHostAndPort(t, "tcp://shard-01-sas.test.vertis.yandex.net:3631s0", "shard-01-sas.test.vertis.yandex.net", 0,
		errors.New("invalid address 'shard-01-sas.test.vertis.yandex.net:3631s0': port is not a number"))
}

func TestGetIPv6FromIPv4(t *testing.T) {
	okIP := net.ParseIP("87.250.250.242")
	expectedIPsNAT64 := []net.IP{}
	expectedIPsNAT64 = append(expectedIPsNAT64, net.ParseIP("2a02:6b8::2:242"))

	expectedIPs := []net.IP{}
	expectedIPs = append(expectedIPs, net.ParseIP("2a02:6b8::2:242"), net.ParseIP("87.250.250.242"))

	ips, err := ResolveAddressFromIPv4(okIP)
	if err != nil {
		t.Errorf("Failed to resolve IPv4-address %s to multiple ips: '%s'", okIP, err)
	}

	if !reflect.DeepEqual(expectedIPsNAT64, ips) && !reflect.DeepEqual(expectedIPs, ips) {
		t.Errorf("Failed to resolve IPv4-address %s to multiple ips, got unexpected result: '%s'", okIP, ips)
	}

}

func TestResolveAddress(t *testing.T) {
	okTestAddress := "ipv6.google.com:443"
	validAddr := []string{"2a00:1450:400f:80d::200e"}
	validPort := int64(443)
	badTestAddress := "https://realty.yandex.ru:443"

	addr, port, err := ResolveAddress(okTestAddress)
	if err != nil {
		t.Errorf("Failed to resolve address %s: '%s'", okTestAddress, err)
	}

	if reflect.DeepEqual(addr, validAddr) || port != validPort {
		t.Errorf("Failed to resolve address %s, got unexpected values, '%s'", okTestAddress, err)
	}

	addr, port, err = ResolveAddress(badTestAddress)
	expectedErr := errors.New("invalid address 'https://realty.yandex.ru:443': address and port should be separated by semicolon")
	assert.Equal(t, expectedErr, err)
}
