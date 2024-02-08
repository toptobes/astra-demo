from functools import reduce
from typing import List

from colbert.indexing.collection_encoder import CollectionEncoder
from colbert.infra.config import ColBERTConfig
from colbert.modeling.checkpoint import Checkpoint

cf = ColBERTConfig(checkpoint='checkpoints/colbertv2.0')
cp = Checkpoint(cf.checkpoint, colbert_config=cf)
encoder = CollectionEncoder(cf, cp)

def encode_passages(texts: List[str]) -> List[List[List[float]]]:
  embeddings_flat, counts = encoder.encode_passages(texts)

  ranges = reduce(
    lambda acc, x: acc + [(x[0], x[0] + x[1])],
    zip([0] + counts, counts),
    [],
  )

  return [embeddings_flat[start:end].tolist() for start, end in ranges]

def encode_query(text: str) -> List[List[float]]:
  return cp.queryFromText([text])[0].tolist()
