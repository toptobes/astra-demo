import SockJS, { CloseEvent } from "sockjs-client";
import Stomp from "stompjs"
import { IndexRequest, SimilarityRequest, SimilarityResult } from "./messages.ts";
import { stopMainLoop } from "../columns.ts";

let stompClient: Stomp.Client = null as any;

export function setupStomp() {
  const socket = new SockJS('http://localhost:8081/ws');
  stompClient = Stomp.over(socket);

  // @ts-ignore
  stompClient.debug = null

  stompClient.connect({}, _ => {
    stompClient.subscribe('/topic/query-result', (msg) => {
      queryListeners.forEach(cb => cb(JSON.parse(msg.body)));
    });
  });

  socket.onclose = (e: CloseEvent) => {
    console.log(e);
    stopMainLoop();
    document.querySelector<HTMLDivElement>('.errored-page')!.classList.remove('hidden');
  }
}

export function indexRequest(batches: IndexRequest[]): void {
  chunk(batches).forEach(batch => {
    stompClient.send('/app/index', {}, JSON.stringify(batch));
  });
}

const queryListeners: ((results: SimilarityResult[]) => void)[] = [];

export function similarityRequest(msg: SimilarityRequest) {
  stompClient.send('/app/query', {}, JSON.stringify(msg));
}

export function onSimilarityResult(cb: (results: SimilarityResult[]) => void) {
  queryListeners.push(cb)
}

const CHARS_PER_CHUNK = 12500;

function chunk(batches: IndexRequest[]): IndexRequest[][] {
  let chunk: IndexRequest[] = [];
  const chunks: IndexRequest[][] = [chunk];
  let chunkLength = 0;

  for (const batch of batches) {
    const sentenceLength = batch.text.length;
    const newChunkLength = chunkLength + +!!chunk.length + sentenceLength;

    if (newChunkLength > CHARS_PER_CHUNK && chunk.length) {
      chunk = [batch];
      chunks.push(chunk);
      chunkLength = sentenceLength;
    } else {
      chunk.push(batch);
      chunkLength = newChunkLength;
    }
  }

  return chunks;
}
