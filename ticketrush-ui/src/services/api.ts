import axios from "axios";

export const BASE_URL = import.meta.env.VITE_API_URL ?? "http://localhost:8080";
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
    const response = await axios.post(`${API_URL}/buy`, {
        userId: userId,
        eventId: eventId,
        amount: 1 
    }, { headers: headers() });
    return response.data;
};

export const getStock = async (eventId: number): Promise<number> => {
    const response = await axios.get(`${API_URL}/events/${eventId}/stock`, { headers: headers() });
    return response.data.stock;
};
