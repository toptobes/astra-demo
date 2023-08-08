import os
from time import time

from flask import Flask, request, jsonify
from transformers import AutoTokenizer, AutoModel
from torch import Tensor
import torch.nn.functional as f
from typing import List

app = Flask(__name__)

tokenizers = {
    'base_v2': AutoTokenizer.from_pretrained("intfloat/e5-base-v2"),
}

models = {
    'base_v2': AutoModel.from_pretrained("intfloat/e5-base-v2"),
}

def average_pool(last_hidden_states: Tensor, attention_mask: Tensor) -> Tensor:
    last_hidden_sum = last_hidden_states.masked_fill(
        ~attention_mask[..., None].bool(), 0.0
    ).sum(dim=1)

    attention_mask_sum = attention_mask.sum(dim=1)[..., None]

    return last_hidden_sum / attention_mask_sum

def get_embedding(text: str, model: str) -> List[float]:
    inputs = tokenizers[model](text, return_tensors="pt", max_length=512, truncation=True)
    outputs = models[model](**inputs)

    embeddings = average_pool(outputs.last_hidden_state, inputs['attention_mask'])
    return f.normalize(embeddings, p=2, dim=1).tolist()[0]

@app.route('/embed', methods=['POST'])
def embed():
    data = request.get_json(force=True)

    texts = data['texts']
    model = data['model']

    embeddings_list = [get_embedding(text, model) for text in texts]

    return jsonify(embeddings_list)

if __name__ == '__main__':
    app_port = os.getenv('ASTRA_DEMO_EMBEDDING_SERVICE_PORT')

    if app_port is None:
        app_port = 5000
    else:
        try:
            app_port = int(app_port)
        except ValueError:
            print("Environment variable MY_ENV_VAR is not a valid integer")
            app_port = 5000  # default to 5000 if the env var is not a valid integer

    app.run(port=app_port)
