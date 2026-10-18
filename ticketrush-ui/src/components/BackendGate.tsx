import { useEffect, useState, type ReactNode } from "react";
import { MAX_WAIT_MS, startWake, useWakeState } from "../services/backendWake";

/** Estimated wake time, only used to pace the bar; the real signal is the health check. */
const ESTIMATE_S = 60;

const WakeUpScreen = () => {
    const { phase, startedAt } = useWakeState();
    const [now, setNow] = useState(() => Date.now());

    useEffect(() => {
        const id = setInterval(() => setNow(Date.now()), 1000);
        return () => clearInterval(id);
    }, []);

    const elapsed = Math.max(0, Math.round((now - startedAt) / 1000));
    const failed = phase === "failed";
    // Eases toward 95% so it never claims to be finished before the server answers.
    const pct = failed ? 100 : Math.min(95, Math.round(100 * (1 - Math.exp(-elapsed / (ESTIMATE_S / 1.6)))));

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-gray-900 p-6 text-white">
            <div className="flex w-full max-w-md flex-col items-center gap-6 rounded-2xl border border-gray-700 bg-gray-800 p-8 text-center shadow-2xl">
                <div
                    aria-hidden="true"
                    className="h-11 w-11 animate-spin rounded-full border-4 border-gray-700 border-t-green-400 motion-reduce:animate-none motion-reduce:border-r-green-400"
                />
                <div role="status" aria-live="polite">
                    <h1 className="mb-2 text-2xl font-bold">
                        {failed ? "Sunucu henüz yanıt vermedi" : "Sunucu uyanıyor"}
                    </h1>
                    <p className="text-sm leading-relaxed text-gray-400">
                        {failed
                            ? `${Math.round(MAX_WAIT_MS / 60000)} dakikadır bekledik ama sunucu cevap vermedi. Biraz sonra tekrar deneyebilirsiniz.`
                            : "Ücretsiz demo sunucusu bir süre kullanılmayınca uykuya geçer. Yeniden açılması bir dakikaya kadar sürebilir; hazır olunca otomatik devam edeceğiz."}
                    </p>
                </div>
                <div
                    role="progressbar"
                    aria-label="Sunucu uyanma durumu"
                    aria-valuemin={0}
                    aria-valuemax={100}
                    aria-valuenow={pct}
                    className="h-2 w-full overflow-hidden rounded-full bg-gray-700"
                >
                    <div
                        className={`h-full rounded-full transition-[width] duration-1000 ease-linear motion-reduce:transition-none ${
                            failed ? "bg-red-500" : "bg-gradient-to-r from-green-400 to-blue-500"
                        }`}
                        style={{ width: `${pct}%` }}
                    />
                </div>
                <div className="flex w-full items-center justify-between font-mono text-sm text-gray-400">
                    <span>{elapsed} sn</span>
                    <button
                        type="button"
                        onClick={startWake}
                        className="rounded-lg border border-gray-600 px-4 py-2 font-sans text-white hover:bg-gray-700 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-green-400"
                    >
                        Tekrar dene
                    </button>
                </div>
            </div>
        </div>
    );
};

/** Starts health polling on load and covers the app with the wake-up screen while the API is not answering. */
const BackendGate = ({ children }: { children: ReactNode }) => {
    const { phase } = useWakeState();

    useEffect(() => {
        startWake();
    }, []);

    return (
        <>
            {children}
            {(phase === "waking" || phase === "failed") && <WakeUpScreen />}
        </>
    );
};

export default BackendGate;
