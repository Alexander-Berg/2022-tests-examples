package mock

import (
	"fmt"

	"github.com/YandexClassifieds/shiva/common"
	"github.com/YandexClassifieds/shiva/pkg/i/kv"
)

type MemoryKV struct {
	data map[string]kv.Value
}

func NewKVMock() *MemoryKV {
	return &MemoryKV{
		data: map[string]kv.Value{},
	}
}

func (m *MemoryKV) Create(k string, v kv.Value) error {
	if _, ok := m.data[k]; ok {
		return fmt.Errorf("key already exists")
	}
	m.data[k] = v
	return nil
}

func (m *MemoryKV) Save(k string, v kv.Value) error {
	m.data[k] = v
	return nil
}

func (m *MemoryKV) Get(k string) (kv.Value, error) {
	v, ok := m.data[k]
	if !ok {
		return nil, common.ErrNotFound
	}
	return v, nil
}

func (m *MemoryKV) All() (map[string]kv.Value, error) {
	return m.data, nil
}
