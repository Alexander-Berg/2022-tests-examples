package parser

import (
	"bytes"
	"testing"

	"github.com/YandexClassifieds/shiva/common"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestParseErr(t *testing.T) {
	_, err := parse(test.NewLogger(t), common.Prod, bytes.NewBufferString(corruptManifest).Bytes())
	require.Error(t, err)
	assert.Equal(t, "cannot parse data for layer 'Prod' as map", err.Error())
}

var (
	corruptManifest = `name: wtf
prod:
 - stuff
 - 1`
)
