package lib

import (
	"fmt"
	flag "github.com/spf13/pflag"
	"os"
	"path"

	"a.yandex-team.ru/library/go/test/recipe"
)

type Recipe struct{}

func (r Recipe) Start() error {
	var (
		fileName string
		filePath string
		fileSize int
	)

	flag.StringVar(&fileName, "name", "tmp_file.recipe", "name of tmp file")
	flag.IntVar(&fileSize, "size", 0, "size of tmp file")
	flag.Parse()

	filePath = path.Join("/tmp", fileName)

	f, err := os.Create(filePath)
	if err != nil {
		return fmt.Errorf("TMP_FILE_RECIPE: can't create file: %v", err)
	}

	if fileSize != 0 {
		err = f.Truncate(int64(fileSize))
		if err != nil {
			return fmt.Errorf("TMP_FILE_RECIPE: can't truncate file: %v", err)
		}
	}

	recipe.SetEnv("RECIPE_TMP_FILE_PATH", filePath)

	return err
}

func (r Recipe) Stop() error {
	filePath := os.Getenv("RECIPE_TMP_FILE_PATH")
	if filePath == "" {
		return fmt.Errorf("TMP_FILE_RECIPE: RECIPE_TMP_FILE_PATH env var is empty")
	}

	err := os.Remove(filePath)
	if err != nil && !os.IsNotExist(err) {
		return fmt.Errorf("TMP_FILE_RECIPE: can't remove file: %v", err)
	}

	return err
}
