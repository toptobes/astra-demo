import SockJS, { CloseEvent } from "sockjs-client";
import Stomp from "stompjs"
import { IndexRequest, SimilarityRequest, SimilarityResult } from "./messages.ts";
import { stopMainLoop } from "../columns.ts";

let stompClient: Stomp.Client = null as any;

export function setupStomp() {
  const socket = new SockJS(import.meta.env.VITE_BACKEND_URL + 'ws');
  stompClient = Stomp.over(socket);

  // @ts-expect-error It can be null just fine
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

const queryListeners: ((results: SimilarityResult) => void)[] = [];

export function similarityRequest(msg: SimilarityRequest) {
  stompClient.send('/app/query', {}, JSON.stringify(msg));
}

export function onSimilarityResult(cb: (results: SimilarityResult) => void) {
  queryListeners.push(cb)
}

const CHARS_PER_CHUNK = parseInt(import.meta.env.VITE_CHARS_PER_CHUNK!);

function chunk(requests: IndexRequest[]): IndexRequest[][] {
  const chunks: IndexRequest[][] = [[]];
  let currentChunkBytes = 0;

  for (let request of requests) {
    let requestBytes = getByteSize(JSON.stringify(request));

    if (currentChunkBytes + requestBytes > CHARS_PER_CHUNK) {
      chunks.push([]);
      currentChunkBytes = 0;
    }

    chunks[chunks.length - 1].push(request);
    currentChunkBytes += requestBytes;
  }

  return chunks;
}

function getByteSize(str: string): number {
  return decodeURIComponent(encodeURIComponent(str)).length;
}
