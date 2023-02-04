package main

import (
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"

	"testing"
)

func TestProcess(t *testing.T) {
	vaultService := &VaultServiceMock{}

	conf := `
qwe = ${QWE}
zxc {
  asd1 = ${?ASD1}
  asd2 = ${?ASD2}
  jdbc = ${?JDBC}
  asd3 = "0"
}
`

	wantedHocon := `
qwe = "123"
zxc {
  asd1 = "456"
  asd2 = "789"
  jdbc = "jdbc:postgresql://vla-ihg8p2yi1n7ik2fy.db.yandex.net.:6432/promo_dispenser?prepareThreshold=0s"
  asd3 = "0"
}
`

	vaultService.On("GetVersion", "ver-qwe").Return(map[string]string{"qwe": "123"}, nil)
	vaultService.On("GetVersion", "ver-asd1").Return(map[string]string{"zxc.asd": "456"}, nil)
	vaultService.On("GetVersion", "ver-asd2").Times(0)

	secrets := map[string]string{
		"QWE":  "${sec-0:ver-qwe:qwe}",
		"ASD1": "${sec-0:ver-asd1:zxc.asd}",
		"ASD2": "789",
		"JDBC": "jdbc:postgresql://pg-rw-mdbp9if6ujmqomn5k6kp.query.consul:6432/promo_dispenser?prepareThreshold=0s",
	}

	processedEnv := resolveEnvs(secrets, vaultService, true)

	got := process(conf, processedEnv)

	assert.Equal(t, wantedHocon, got)
}

type VaultServiceMock struct {
	mock.Mock
}

func (service *VaultServiceMock) GetVersion(version string) (map[string]string, error) {
	args := service.Called(version)
	return args.Get(0).(map[string]string), args.Error(1)
}
