import axios from "axios";

export const BASE_URL = import.meta.env.VITE_API_URL ?? "http://localhost:8080";
const API_URL = `${BASE_URL}/api/tickets`;

export const buyTicket = async (userId: number, eventId: number) => {
    const response = await axios.post(`${API_URL}/buy`, {
        userId: userId,
        eventId: eventId,
        amount: 1 
    });
    return response.data;
};