import fs from 'fs-extra';
import path from 'path';

const dimMochawesomeReportDir = path.join(__dirname, '..', 'mochawesome-report');

const getPaths = (dir: string, file: string): Array<string> => {
  const filePathName = path.join(dir, file);
  const isDir = fs.lstatSync(filePathName).isDirectory();

  if (isDir) {
    const paths: Array<string> = [];

    const files = fs.readdirSync(filePathName);

    files.forEach(file => {
      paths.push(...getPaths(filePathName, file));
    });

    return paths;
  }

  return [filePathName];
};

const moveFiles = (folder: string): void => {
  try {
    const dir = path.join(__dirname, folder);
    const dirNew = path.join(dimMochawesomeReportDir, folder);
    fs.copySync(dir, dirNew);

    const paths = getPaths(dirNew, '');
    fs.writeFileSync(path.join(dirNew, 'index.json'), JSON.stringify(paths, null, 2));

    fs.writeFileSync(
      path.join(dirNew, 'index.html'),
      `
      <body>
      ${paths.reduce((acc, pathWrap) => {
        const path = pathWrap.replace(dirNew, '.');

        return `
            ${acc}
              <p>
                <a href=${encodeURI(path)}>${path}</a>
              </p>`;
      }, '')}
      </body>
      `,
    );
  } catch (error) {
    console.error(error);
  }
};

moveFiles('videos');
moveFiles('screenshots');
