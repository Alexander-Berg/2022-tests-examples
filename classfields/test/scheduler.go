package test

import (
	"encoding/json"

	"github.com/YandexClassifieds/shiva/pkg/i/kv"
)

const (
	TestInfoKey = "test-info"
	CINamespace = "test-info"
)

// TestInfo - sharing data between scheduler and test job
type TestInfo struct {
	Attempts []string `json:"attempts"`
	Hosts    []string `json:"hosts"`
	OOM      bool     `json:"oom"`
}

func (t *TestInfo) New() kv.Value {
	return &TestInfo{}
}

func (t *TestInfo) Marshal() ([]byte, error) {
	return json.Marshal(t)
}

func (t *TestInfo) Unmarshal(bytes []byte) error {
	return json.Unmarshal(bytes, t)
}
