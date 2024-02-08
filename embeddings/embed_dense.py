from typing import List, Union, Literal

import torch.nn.functional as f
from torch import Tensor
from transformers import AutoTokenizer, AutoModel

import env

model = AutoModel.from_pretrained(env.DENSE_MODEL).to(env.DEVICE)
tokenizer = AutoTokenizer.from_pretrained(env.DENSE_MODEL)

def average_pool(last_hidden_states: Tensor, attention_mask: Tensor) -> Tensor:
	last_hidden_sum = last_hidden_states.masked_fill(
		~attention_mask[..., None].bool(), 0.0
	).sum(dim=1)

	attention_mask_sum = attention_mask.sum(dim=1)[..., None]

	return last_hidden_sum / attention_mask_sum

def encode(text: str, prefix: Union[Literal["passage"], Literal["query"]]) -> List[float]:
	inputs = tokenizer(prefix + ": " +text, return_tensors="pt", max_length=512, truncation=True)
	inputs = { key: tensor.to(env.DEVICE) for key, tensor in inputs.items() }

	outputs = model(**inputs)

	embeddings = average_pool(outputs.last_hidden_state, inputs['attention_mask'])
	return f.normalize(embeddings, p=2, dim=1).cpu().tolist()[0]

def encode_passages(texts: List[str]) -> List[List[float]]:
	return [encode(text, "passage") for text in texts]

def encode_query(text: str) -> List[float]:
	return encode(text, "query")
