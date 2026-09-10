import { Injectable, signal, OnDestroy } from '@angular/core';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import { environment } from '../../../environments/environment';

export interface WsMessage<T = any> {
  type: string;
  payload: T;
}

@Injectable({ providedIn: 'root' })
export class WebsocketService implements OnDestroy {
  private client: Client;
  private activeSubscriptions = new Map<string, { topic: string; callback: (data: any) => void; sub?: StompSubscription }>();
  private subCounter = 0;

  // Real-time connection status signal
  readonly connected = signal<boolean>(false);

  constructor() {
    // Determine native ws:// URL from environment.wsUrl (e.g. http://localhost:8080/ws)
    const wsUrl = environment.wsUrl.replace(/^http/, 'ws');

    this.client = new Client({
      brokerURL: wsUrl,
      reconnectDelay: 3000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      debug: (msg: string) => {
        // Uncomment for troubleshooting: console.log('[STOMP]', msg);
      }
    });

    this.client.onConnect = () => {
      console.log('[WebSocket] Real-time STOMP serverga ulandi:', wsUrl);
      this.connected.set(true);

      // Re-establish all registered subscriptions
      this.activeSubscriptions.forEach((item, id) => {
        this.bindSubscription(id, item);
      });
    };

    this.client.onDisconnect = () => {
      console.warn('[WebSocket] Real-time aloqa uzildi');
      this.connected.set(false);
    };

    this.client.onStompError = (frame) => {
      console.error('[WebSocket] STOMP xatosi:', frame.headers['message'], frame.body);
      this.connected.set(false);
    };

    this.client.onWebSocketClose = () => {
      this.connected.set(false);
    };

    // Start connection
    this.client.activate();
  }

  ngOnDestroy(): void {
    this.disconnect();
  }

  disconnect(): void {
    if (this.client) {
      this.client.deactivate();
    }
    this.connected.set(false);
  }

  /**
   * Subscribe to a general topic (e.g. /topic/kitchen/{kitchenId})
   * Returns an unsubscribe function.
   */
  subscribe<T = any>(topic: string, callback: (data: T) => void): () => void {
    const subId = `sub_${++this.subCounter}`;
    const subItem = { topic, callback };
    this.activeSubscriptions.set(subId, subItem);

    if (this.connected() && this.client.connected) {
      this.bindSubscription(subId, subItem);
    }

    return () => {
      const item = this.activeSubscriptions.get(subId);
      if (item?.sub) {
        try {
          item.sub.unsubscribe();
        } catch (e) {
          // ignore
        }
      }
      this.activeSubscriptions.delete(subId);
    };
  }

  /**
   * Dedicated helper for kitchen station channels (/topic/kitchen/{kitchenId})
   */
  subscribeToKitchen(kitchenId: string, callback: (event: WsMessage) => void): () => void {
    const topic = `/topic/kitchen/${kitchenId}`;
    return this.subscribe(topic, callback);
  }

  private bindSubscription(subId: string, item: { topic: string; callback: (data: any) => void; sub?: StompSubscription }): void {
    try {
      item.sub = this.client.subscribe(item.topic, (message: IMessage) => {
        try {
          const parsed = JSON.parse(message.body);
          item.callback(parsed);
        } catch (e) {
          item.callback(message.body);
        }
      });
      console.log(`[WebSocket] Kanalga obuna bo'lindi: ${item.topic}`);
    } catch (err) {
      console.error(`[WebSocket] Obuna xatosi (${item.topic}):`, err);
    }
  }
}
