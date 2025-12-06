import { Client } from "@stomp/stompjs";
import { useEffect, useRef, useState } from "react";
import SockJS from "sockjs-client";
import { buyTicket } from "../services/api";

interface NotificationMessage {
    id: string;
    content: string;
    timestamp: string;
}

const Dashboard = () => {
    const [notifications, setNotifications] = useState<NotificationMessage[]>([]);
    const [status, setStatus] = useState<string>("Bağlanıyor...");
    const [loading, setLoading] = useState(false);

    const [myUserId] = useState(Math.floor(Math.random() * 1000) + 1);

    const stompClientRef = useRef<Client | null>(null);

    useEffect(() => {
        const socket = new SockJS("http://localhost:8080/ws-ticket");

        const client = new Client({
            webSocketFactory: () => socket,
            onConnect: () => {
                setStatus("Canlı Bağlantı Sağlandı");

                client.subscribe('/topic/notifications', (message) => {
                    const newMsg: NotificationMessage = {
                        id: Date.now().toString() + Math.random().toString(),
                        content: message.body,
                        timestamp: new Date().toLocaleTimeString(),
                    };

                    setNotifications((prev) => [newMsg, ...prev].slice(0, 50));
                });
            },
            onDisconnect: () => {
                setStatus("Bağlantı Kesildi"); 
            },
            onStompError: (frame) => {
                console.error('Broker hatası: ' , frame.headers['message']);
                setStatus("Bağlantı Hatası Oluştu");
            }
        });

        client.activate();
        stompClientRef.current = client;

        return () => {
            client.deactivate();
        };
    }, []);

  const handleBuy = async () => {
        setLoading(true);
        try {
            await buyTicket(myUserId, 1);
        } catch (error: any) {
            console.error("Hata Detayı:", error);
            
            let errorMessage = "Beklenmedik bir hata oluştu.";

            if (error.response && error.response.data && error.response.data.message) {
                errorMessage = error.response.data.message; 
            } else if (error.message) {
                errorMessage = error.message;
            }
            alert("İŞLEM BAŞARISIZ: \n" + errorMessage);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="min-h-screen bg-gray-900 text-white p-10 flex flex-col items-center gap-8 font-sans">
            <h1 className="text-4xl font-bold text-transparent bg-clip-text bg-gradient-to-r from-green-400 to-blue-500">
                TicketRush Live Monitor
            </h1>
            
            <div className="text-gray-400 font-mono text-sm">{status}</div>

            <div className="bg-gray-800 p-6 rounded-2xl shadow-2xl w-full max-w-md border border-gray-700 hover:border-gray-600 transition-colors">
                <div className="flex justify-between items-center mb-4">
                    <h2 className="text-2xl font-bold">Tarkan Konseri</h2>
                    <span className="bg-blue-600 text-xs px-2 py-1 rounded-full font-semibold">EVENT #1</span>
                </div>
                <p className="text-gray-400 mb-6 flex justify-between">
                    <span>Fiyat:</span>
                    <span className="text-green-400 font-bold">500.00 TL</span>
                </p>
                
                <button 
                    onClick={handleBuy}
                    disabled={loading}
                    className={`w-full py-3 rounded-lg font-bold transition-all transform active:scale-95 ${
                        loading 
                        ? 'bg-gray-600 cursor-not-allowed text-gray-300' 
                        : 'bg-green-600 hover:bg-green-500 text-white shadow-lg shadow-green-900/50'
                    }`}
                >
                    {loading ? 'İşleniyor...' : `🎫 Bilet Al (User: ${myUserId})`}
                </button>
            </div>

            <div className="w-full max-w-2xl mt-4">
                <h3 className="text-xl font-semibold mb-3 border-b border-gray-700 pb-2 flex items-center gap-2">
                     Canlı İşlem Akışı
                </h3>
                
                <div className="bg-black/80 rounded-xl p-4 h-80 overflow-y-auto border border-gray-700 font-mono text-sm space-y-2 shadow-inner custom-scrollbar">
                    {notifications.length === 0 && (
                        <div className="text-gray-500 text-center mt-20 italic">Henüz işlem yok... Bekleniyor...</div>
                    )}
                    
                    {notifications.map((msg) => (
                        <div key={msg.id} className="flex gap-3 items-start animate-pulse">
                            <span className="text-gray-500 text-xs mt-0.5">[{msg.timestamp}]</span>
                            <span className="text-green-500">➜</span>
                            <span className="text-gray-200">{msg.content}</span>
                        </div>
                    ))}
                </div>
            </div>
        </div>
    );
};

export default Dashboard;