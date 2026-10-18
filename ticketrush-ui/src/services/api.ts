import axios from "axios";
import { BASE_URL } from "./config";
import { reportBackendDown, getWakePhase, waitUntilReady } from "./backendWake";

export { BASE_URL };

const NORMAL_TIMEOUT_MS = 30_000;
// A request that had to wait for the server to wake may hit a cold database right after.
const WAKE_TIMEOUT_MS = 60_000;
const MAX_WAKE_RETRIES = 3;

const http = axios.create();

http.interceptors.request.use(async (config) => {
    // Hold every request back while the free server is asleep instead of letting it fail.
    if (getWakePhase() !== "ready") {
        await waitUntilReady();
        config.timeout = WAKE_TIMEOUT_MS;
    } else {
        config.timeout = config.timeout || NORMAL_TIMEOUT_MS;
    }
    return config;
});

http.interceptors.response.use(undefined, async (error) => {
    const config = error.config;
    const status = error.response?.status;
    // Server not there (sleeping / booting): network error, timeout, or the proxy's 502-504.
    if (config && (!error.response || status === 502 || status === 503 || status === 504)) {
        reportBackendDown();
        const retries = config._wakeRetries ?? 0;
        if (config.method?.toLowerCase() === "get" && retries < MAX_WAKE_RETRIES) {
            config._wakeRetries = retries + 1;
            await waitUntilReady();
            return http(config);
        }
    }
    return Promise.reject(error);
});
const API_URL = `${BASE_URL}/api/tickets`;

const VISITOR_KEY = "ticketrush-visitor-id";
let memoryVisitorId: string | null = null;

const randomId = (): string =>
    typeof crypto !== "undefined" && "randomUUID" in crypto
        ? crypto.randomUUID()
        : `${Date.now().toString(16)}-${Math.random().toString(16).slice(2)}-visitor`;

// Stable per browser: localStorage first, then sessionStorage, then memory (private modes can block storage).
export const getVisitorId = (): string => {
    if (memoryVisitorId) return memoryVisitorId;
    for (const store of [() => window.localStorage, () => window.sessionStorage]) {
        try {
            const s = store();
            const existing = s.getItem(VISITOR_KEY);
            if (existing) return (memoryVisitorId = existing);
            const created = randomId();
            s.setItem(VISITOR_KEY, created);
            return (memoryVisitorId = created);
        } catch {
            // try the next storage
        }
    }
    return (memoryVisitorId = randomId());
};

const headers = () => ({ "X-Visitor-Id": getVisitorId() });

export const buyTicket = async (userId: number, eventId: number) => {
    const response = await http.post(`${API_URL}/buy`, {
        userId: userId,
        eventId: eventId,
        amount: 1 
    }, { headers: headers() });
    return response.data;
};

export const getStock = async (eventId: number): Promise<number> => {
    const response = await http.get(`${API_URL}/events/${eventId}/stock`, { headers: headers() });
    return response.data.stock;
};
