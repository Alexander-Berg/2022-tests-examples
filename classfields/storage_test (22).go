package data

import (
	"strings"
	"testing"

	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/stretchr/testify/assert"
)

func TestStorage_SaveAndGet(t *testing.T) {

	test.RunUp(t)
	defer test.Clean(t)
	s := NewStorage(test_db.NewDb(t), test.NewLogger(t))
	d := &Data{
		Path:       "path",
		Info:       []byte{1, 2},
		Checksum:   "sum",
		CommitHash: "hash",
	}
	test.Check(t, s.Save(d))

	getD, err := s.Get(d.ID)
	test.Check(t, err)
	assert.Equal(t, d.Path, getD.Path)
	assert.Equal(t, d.Info, getD.Info)
	assert.Equal(t, strings.TrimSpace(d.Checksum), strings.TrimSpace(getD.Checksum))
	assert.Equal(t, d.CommitHash, getD.CommitHash)

	test.Check(t, s.base.DB().Delete(getD).Error)
	_, err = s.Get(d.ID)
	test.Check(t, err)
}

func TestStorage_SaveAndGetByPath(t *testing.T) {

	test.RunUp(t)
	defer test.Clean(t)
	s := NewStorage(test_db.NewDb(t), test.NewLogger(t))
	d := &Data{
		Path:       "path",
		Info:       []byte{1, 2},
		Checksum:   "sum",
		CommitHash: "hash",
	}
	test.Check(t, s.Save(d))

	getD, err := s.GetByPath("path")
	test.Check(t, err)
	assert.Equal(t, d.Path, getD.Path)
	assert.Equal(t, d.Info, getD.Info)
	assert.Equal(t, strings.TrimSpace(d.Checksum), strings.TrimSpace(getD.Checksum))
	assert.Equal(t, d.CommitHash, getD.CommitHash)

}
