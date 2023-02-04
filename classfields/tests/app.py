import flask

app = flask.Flask("test_app", static_url_path="", static_folder="")


@app.route("/xml")
def serve_xml():
    return app.send_static_file("slnixon.xml")


if __name__ == "__main__":
    app.run()
