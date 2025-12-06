import axios from "axios";

const API_URL = "http://localhost:8080/api/tickets";

export const buyTicket = async (userId: number, eventId: number) => {
    const response = await axios.post(`${API_URL}/buy`, {
        userId: userId,
        eventId: eventId,
        amount: 1 
    });
    return response.data;
};