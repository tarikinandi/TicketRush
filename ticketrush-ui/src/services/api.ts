import axios from "axios";

const API_URL = "http://localhost:8080/api/tickets";

export const buyTicket = async (userId: number, eventId: number) => {
    const response = await axios.post(`${API_URL}/buy`, null, {
        params: { userId, eventId },
    });
    return response.data;
};