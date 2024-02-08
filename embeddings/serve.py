from flask import Flask, request, jsonify

import env
import embed_colbert
import embed_dense

app = Flask(__name__)

@app.route('/embed/passages', methods=['POST'])
def embed_passages():
	data = request.get_json(force=True)
	texts = data['texts']

	return jsonify({
		'dense': embed_dense.encode_passages(texts),
		'multi': embed_colbert.encode_passages(texts)
	})

@app.route('/embed/query', methods=['POST'])
def embed_query():
	data = request.get_json(force=True)
	text = data['text']

	return jsonify({
		'dense': embed_dense.encode_query(text),
		'multi': embed_colbert.encode_query(text)
	})

if __name__ == '__main__':
	app.run(port=env.APP_PORT)
