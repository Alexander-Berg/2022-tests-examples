package app

import (
	"github.com/YandexClassifieds/h2p/cmd/cli/errors"
	"github.com/sirupsen/logrus"
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestFreePort(t *testing.T) {
	a := App{}
	a.log = logrus.New()
	port, err := a.GetFreePortAbove(3000)
	assert.NoError(t, err)
	assert.True(t, port >= 3000)
}

func TestBigPort(t *testing.T) {
	a := App{}
	a.log = logrus.New()
	_, err := a.GetFreePortAbove(70000)
	assert.Equal(t, err, errors.NoAvailableFreePorts)
}
