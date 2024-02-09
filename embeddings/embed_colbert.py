import itertools
from typing import List

from colbert.indexing.collection_encoder import CollectionEncoder
from colbert.infra.config import ColBERTConfig
from colbert.modeling.checkpoint import Checkpoint

cf = ColBERTConfig(checkpoint='checkpoints/colbertv2.0')
cp = Checkpoint(cf.checkpoint, colbert_config=cf)
encoder = CollectionEncoder(cf, cp)

def encode_passages(texts: List[str]) -> List[List[List[float]]]:
  embeddings_flat, counts = encoder.encode_passages(texts)

  start_indices = [0] + list(itertools.accumulate(counts[:-1]))

  return [embeddings_flat[start:start + count].tolist() for start, count in zip(start_indices, counts)]

def encode_query(text: str) -> List[List[float]]:
  return cp.queryFromText([text])[0].tolist()
