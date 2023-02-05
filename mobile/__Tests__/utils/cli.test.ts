import { CLI } from '../../src/utils/cli'

describe(CLI, () => {
  it('parses input directory with flag -i', () => {
    expect(CLI.parse(['', 'app.js', '-i', './inDir', '-o', './outDir'])).toMatchObject({
      inputDirectory: './inDir',
    })
  })

  it('parses output directory with flag -o', () => {
    expect(CLI.parse(['', 'app.js', '-i', './inDir', '-o', './outDir'])).toMatchObject({
      outputDirectory: './outDir',
    })
  })

  it('parses generator directory with flag -g', () => {
    expect(CLI.parse(['', 'app.js', '-i', './inDir', '-o', './outDir', '-g', './gen'])).toMatchObject({
      generatorDirectory: './gen',
    })
  })

  it('uses ./generator if generator path is not specified', () => {
    expect(CLI.parse(['', 'app.js', '-i', './inDir', '-o', './outDir'])).toMatchObject({
      generatorDirectory: './generator',
    })
  })

  it('parses config file path with flag -c', () => {
    expect(CLI.parse(['', 'app.js', '-i', './inDir', '-o', './outDir', '-c', './folder/cfg.json'])).toMatchObject({
      configPath: './folder/cfg.json',
    })
  })

  it('uses ./generator/config.json if config path is not specified', () => {
    expect(CLI.parse(['', 'app.js', '-i', './inDir', '-o', './outDir', '-g', './gen'])).toMatchObject({
      generatorDirectory: './gen',
      configPath: 'gen/config.json',
    })
  })

  it('skips cache file generation if -s is set', () => {
    expect(CLI.parse(['', 'app.js', '-i', './inDir', '-o', './outDir', '-g', './gen', '-s'])).toMatchObject({
      generatorDirectory: './gen',
      configPath: 'gen/config.json',
      cacheFiles: false,
    })
  })

  it('uses cached file if -s is not set', () => {
    expect(CLI.parse(['', 'app.js', '-i', './inDir', '-o', './outDir', '-g', './gen'])).toMatchObject({
      generatorDirectory: './gen',
      configPath: 'gen/config.json',
      cacheFiles: true,
    })
  })

  it('parses all -i, -o, -g, -c and -s parameters', () => {
    expect(
      CLI.parse(['', 'app.js', '-i', './inDir', '-o', './outDir', '-g', './gen', '-c', './config.json', '-s']),
    ).toStrictEqual({
      inputDirectory: './inDir',
      outputDirectory: './outDir',
      generatorDirectory: './gen',
      configPath: './config.json',
      cacheFiles: false,
    })
  })
})
