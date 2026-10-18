import { useSyncExternalStore } from "react";
import { BASE_URL } from "./config";

/**
 * The free demo API sleeps after ~15 min idle and needs up to about a minute to boot. This module
 * tracks whether the backend is reachable, polls /api/health while it is not, and lets the HTTP
 * client and the live connection wait until it is.
 */
export type WakePhase = "checking" | "ready" | "waking" | "failed";

interface WakeState {
    phase: WakePhase;
    /** Epoch ms when the current wake-up attempt started. */
    startedAt: number;
}

export const MAX_WAIT_MS = 180_000;
const SHOW_AFTER_MS = 1_200;
const PROBE_TIMEOUT_MS = 8_000;

let state: WakeState = { phase: "checking", startedAt: Date.now() };
const listeners = new Set<() => void>();

const setState = (phase: WakePhase, startedAt?: number) => {
    state = { phase, startedAt: startedAt ?? state.startedAt };
    listeners.forEach((l) => l());
};
const subscribe = (l: () => void) => {
    listeners.add(l);
    return () => {
        listeners.delete(l);
    };
};

export const getWakePhase = (): WakePhase => state.phase;
export const useWakeState = (): WakeState => useSyncExternalStore(subscribe, () => state);

const probe = async (): Promise<boolean> => {
    const ctrl = new AbortController();
    const timer = setTimeout(() => ctrl.abort(), PROBE_TIMEOUT_MS);
    try {
        // 503 {"status":"STARTING"} while the app boots or seeds, 200 {"status":"UP"} once it is ready.
        const res = await fetch(`${BASE_URL}/api/health`, { cache: "no-store", signal: ctrl.signal });
        return res.ok;
    } catch {
        return false;
    } finally {
        clearTimeout(timer);
    }
};

const sleep = (ms: number) => new Promise((r) => setTimeout(r, ms));
let runId = 0;

/** (Re)starts polling. A newer call supersedes an older loop. */
export const startWake = (): void => {
    const id = ++runId;
    const started = Date.now();
    setState("checking", started);
    const reveal = setTimeout(() => {
        if (id === runId && state.phase === "checking") setState("waking");
    }, SHOW_AFTER_MS);

    void (async () => {
        let delay = 1_500;
        while (id === runId && Date.now() - started < MAX_WAIT_MS) {
            if (await probe()) {
                clearTimeout(reveal);
                if (id === runId) setState("ready");
                return;
            }
            if (id !== runId) return;
            if (state.phase === "checking") setState("waking");
            await sleep(delay);
            delay = Math.min(delay + 500, 3_000);
        }
        clearTimeout(reveal);
        if (id === runId) setState("failed");
    })();
};

/** Called when a request failed in a way that means "server not there". */
export const reportBackendDown = (): void => {
    if (state.phase === "ready") startWake();
};

/** Resolves once the backend is ready, or once waiting has been given up on. */
export const waitUntilReady = (): Promise<void> => {
    if (state.phase === "ready" || state.phase === "failed") return Promise.resolve();
    return new Promise((resolve) => {
        const unsub = subscribe(() => {
            if (state.phase === "ready" || state.phase === "failed") {
                unsub();
                resolve();
            }
        });
    });
};
