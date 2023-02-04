package test_db

import (
	"testing"

	"github.com/YandexClassifieds/shiva/common/storage"
	"github.com/YandexClassifieds/shiva/test"
)

func NewSeparatedDb(t *testing.T) *storage.Database {
	db := test.NewSeparatedGorm(t)
	return storage.NewDatabase(db, test.NewLogger(t), nil, nil, nil)
}

func NewDb(t *testing.T) *storage.Database {
	db := test.NewGorm(t)
	return storage.NewDatabase(db, test.NewLogger(t), nil, nil, nil)
}
