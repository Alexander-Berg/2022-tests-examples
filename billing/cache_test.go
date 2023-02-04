package template

import (
	"context"
	"embed"
	"fmt"
	"testing"

	"github.com/stretchr/testify/assert"
)

//go:embed gotest/sql/*
var testTemplatesFS embed.FS

func TestRender(t *testing.T) {
	ctx := context.Background()
	c := NewCache(testTemplatesFS)
	templatePath := "gotest/sql/test_template.txt"

	res, err := c.Render(ctx, "world", templatePath)
	if err != nil {
		t.Fatal(err)
	}
	assert.Equal(t, res, "Hello, world!\n")

	cImpl := c.(*cacheRenderer)
	assert.Contains(t, cImpl.templates, templatePath)
	assert.Len(t, cImpl.templates, 1)

	res, err = c.Render(ctx, "death", templatePath)
	if err != nil {
		t.Fatal(err)
	}
	assert.Equal(t, res, "Hello, death!\n")
}

func TestRenderComposite(t *testing.T) {
	ctx := context.Background()
	c := NewCache(testTemplatesFS)
	templateMainPath := "gotest/sql/test_template_main.txt"
	templateAddPath := "gotest/sql/test_template_add.txt"

	res, err := c.Render(ctx, "world", templateMainPath, templateAddPath)
	if err != nil {
		t.Fatal(err)
	}
	assert.Equal(t, res, "What do i see here?\nSOME_RANDOM_STUFF_HELLO world\n\n")

	cImpl := c.(*cacheRenderer)
	assert.Contains(t, cImpl.templates, fmt.Sprintf("%s\n%s", templateMainPath, templateAddPath))
	assert.Len(t, cImpl.templates, 1)
}

func TestUnknownTemplate(t *testing.T) {
	ctx := context.Background()
	c := NewCache(testTemplatesFS)
	templatInvalidPath := "gotest/sql/test_template.abyrvalg"

	_, err := c.Render(ctx, "world", templatInvalidPath)
	if assert.Error(t, err) {
		assert.Equal(t, err.Error(), fmt.Sprintf("couldn't get template: no file %s found", templatInvalidPath))
	}

	cImpl := c.(*cacheRenderer)
	assert.Empty(t, cImpl.templates)
}
