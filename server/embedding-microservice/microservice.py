from flask import Flask, request, jsonify
from transformers import AutoTokenizer, AutoModel
from torch import Tensor
import torch.nn.functional as f
from typing import List

app = Flask(__name__)

tokenizer = AutoTokenizer.from_pretrained("intfloat/e5-small-v2")
model = AutoModel.from_pretrained("intfloat/e5-small-v2")

def average_pool(last_hidden_states: Tensor, attention_mask: Tensor) -> Tensor:
    last_hidden_sum = last_hidden_states.masked_fill(
        ~attention_mask[..., None].bool(), 0.0
    ).sum(dim=1)

    attention_mask_sum = attention_mask.sum(dim=1)[..., None]

    return last_hidden_sum / attention_mask_sum

def get_embedding(text: str) -> List[float]:
    inputs = tokenizer(text, return_tensors="pt", max_length=512)
    outputs = model(**inputs)

    embeddings = average_pool(outputs.last_hidden_state, inputs['attention_mask'])
    return f.normalize(embeddings, p=2, dim=1).tolist()[0]

@app.route('/embed', methods=['POST'])
def embed():
    data = request.get_json(force=True)
    texts = data['texts']

    embeddings_list = [get_embedding(text) for text in texts]

    return jsonify(embeddings_list)

if __name__ == '__main__':
    app.run(port=5000)
