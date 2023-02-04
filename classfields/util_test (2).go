package model

import (
	"github.com/YandexClassifieds/logs/api/collector"
	"github.com/francoispqt/gojay"
	"github.com/stretchr/testify/require"
	"testing"
)

func Test_encodeLogContext(t *testing.T) {
	result, err := gojay.MarshalJSONObject(gojay.EncodeObjectFunc(func(enc *gojay.Encoder) {
		encodeLogContext(enc, &collector.LogContext{})
	}))
	require.NoError(t, err)
	_ = result
}
