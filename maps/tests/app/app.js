require('http')
    .createServer((_req, res) => res.end('All is ok'))
    .listen(Number(process.env.MAPS_NODEJS_PORT), (err) => {
        if (err) {
            console.log('Something wrong', err);
            process.exit(1);
        }

        console.log(`Server is listening...`);
    });
