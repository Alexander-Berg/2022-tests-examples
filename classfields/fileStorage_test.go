package storage

import (
	vlogrus "github.com/YandexClassifieds/go-common/log/logrus"
	"github.com/YandexClassifieds/logs/cmd/collector/domain"
	"github.com/YandexClassifieds/logs/cmd/collector/domain/test"
	"github.com/YandexClassifieds/logs/cmd/collector/testutil"
	"github.com/spf13/viper"
	"github.com/stretchr/testify/assert"
	"os"
	"testing"
	"time"
)

func TestBackupData(t *testing.T) {

	testutil.Init(t)
	// run up
	initWs := test.CreateWrappers(t)
	for i, w := range initWs {
		w.SeqNo = uint64(i)
	}

	// main
	store := storage(t)
	store.SaveBackupData(initWs)
	ws := store.LoadBackupData()

	// assert
	i := 0
	for w := range ws {
		assert.NotNil(t, w.Data)
		assert.NotNil(t, w.Data.Body)
		assert.True(t, len(w.Data.Body) > 0)
		assert.Nil(t, w.Data.OnSuccess)
		assert.Nil(t, w.Data.OnFail)
		assert.Equal(t, uint64(i), w.Data.SeqNo)
		assert.Equal(t, w.ServiceName, initWs[i].ServiceName)
		assert.Equal(t, w.AllocationId, initWs[i].AllocationId)
		assert.Equal(t, w.Message, initWs[i].Message)
		i++
	}
	assert.Equal(t, 3, i)

	// assert double request
	time.Sleep(2 * time.Second)
	ws = store.LoadBackupData()
	assert.Equal(t, 0, len(ws))

	//tear down
	removeFiles(t, viper.GetString("FILE_STORAGE_BACKUP_PATH"))
}

func TestMessages(t *testing.T) {

	testutil.Init(t)

	messages := []*domain.Wrapper{
		{Message: []byte(test.Message1), ServiceName: "srv1", AllocationId: "abcdef1"},
		{Message: []byte(test.Message2), ServiceName: "srv2", AllocationId: "abcdef2"},
		{Message: []byte(test.Message3), ServiceName: "srv3", AllocationId: "abcdef3"},
	}

	// main
	store := storage(t)
	for _, m := range messages {
		store.SaveMessage(m)
	}

	// assert
	ws := store.LoadMessages()
	i := 0
	for w := range ws {
		assert.NotNil(t, w.Data)
		assert.NotNil(t, w.Data.Body)
		assert.True(t, len(w.Data.Body) > 0)
		assert.Nil(t, w.Data.OnSuccess)
		assert.Nil(t, w.Data.OnFail)
		assert.Equal(t, uint64(0), w.Data.SeqNo)
		assert.Equal(t, messages[i].Message, w.Message)
		assert.Equal(t, messages[i].ServiceName, w.ServiceName)
		assert.Equal(t, messages[i].AllocationId, w.AllocationId)
		i++
	}
	assert.Equal(t, 3, i)

	// assert double request
	ws = store.LoadMessages()
	assert.Equal(t, 0, len(ws))

	// tear down
	removeFiles(t, viper.GetString("FILE_STORAGE_MESSAGE_PATH"))
}

func TestFailData(t *testing.T) {

	testutil.Init(t)

	// run up
	initWs := test.CreateWrappers(t)

	// main
	store := storage(t)
	store.SaveFailData(initWs)
	ws := store.LoadFailData()

	// assert
	i := 0
	for w := range ws {
		assert.NotNil(t, w.Data)
		assert.NotNil(t, w.Data.Body)
		assert.True(t, len(w.Data.Body) > 0)
		assert.Nil(t, w.Data.OnSuccess)
		assert.Nil(t, w.Data.OnFail)
		assert.Equal(t, uint64(0), w.Data.SeqNo)
		assert.Equal(t, w.ServiceName, initWs[i].ServiceName)
		assert.Equal(t, w.AllocationId, initWs[i].AllocationId)
		assert.Equal(t, w.Message, initWs[i].Message)
		i++
	}
	assert.Equal(t, 3, i)

	// assert double request
	ws = store.LoadFailData()
	assert.Equal(t, 0, len(ws))

	//tear down
	removeFiles(t, viper.GetString("FILE_STORAGE_FAIL_PATH"))
}

func removeFiles(t *testing.T, path string) {

	if _, err := os.Stat(path); !os.IsNotExist(err) {
		err := os.Remove(path)
		if err != nil {
			assert.NoError(t, err)
		}
	}
}

func storage(t *testing.T) Storage {
	s, err := NewFileStorage(vlogrus.New())
	assert.NoError(t, err)
	return s
}
