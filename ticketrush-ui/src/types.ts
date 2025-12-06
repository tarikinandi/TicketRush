export interface TicketEvent {
    id: number;
    name: string;   
    description?: string;
    price: number;
    stock: number;
}

export interface NotificationService {
    id: string;
    content: string;
    timestamp: string;
}