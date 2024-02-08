export interface IndexRequest {
  text: string,
  url?: string,
}

export interface SimilarityRequest {
  query: string,
  limit: number,
}

export interface SimilarityResult {
  dense: IndexRequest[],
  multi: IndexRequest[],
}
