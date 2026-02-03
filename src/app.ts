import dotenv from "dotenv";
dotenv.config();

import express, { Request, Response, NextFunction } from "express";
import morgan from "morgan";
import fs from "fs";
import path from "path";
import cors from "cors";
import axios from "axios";

import { InboundMessage, SynapSysResponse } from "./types";
import { loadEverlybotPrivateConfig } from "./config/privateConfig";

import { Guard } from "./guards/Guard";
import { getClientIp } from "./guards/ip";
import { RateLimitGuard } from "./guards/RateLimitGuard";
import { RequestIdGuard } from "./guards/RequestIdGuard";
import { RequestValidationGuard } from "./guards/RequestValidationGuard";

const privateConfig = loadEverlybotPrivateConfig();

const SENDER_ID = privateConfig.senderId;
const SYNAPSYS_KEY = privateConfig.synapsysClientKey;

const app = express();
const PORT = process.env.PORT || 3000;

const MAX_MESSAGE_LENGTH = Number(process.env.MAX_MESSAGE_LENGTH || 4000);
const JSON_BODY_LIMIT = process.env.JSON_BODY_LIMIT || "16kb";
const RATE_LIMIT_RPM = Number(process.env.RATE_LIMIT_RPM || 60);
const RATE_LIMIT_BURST = Number(process.env.RATE_LIMIT_BURST || 10);

const base = process.env.SYNAPSYS_BASE_URL;
if (!base) throw new Error("Missing env var: SYNAPSYS_BASE_URL");
const synapsysUrl = `${base}/api/v1/chat`;

app.use(express.json({ limit: JSON_BODY_LIMIT }));

app.use(
    cors({
        origin: process.env.ALLOWED_ORIGIN || "*",
        methods: ["POST", "OPTIONS", "GET"],
    })
);

function sendClientError(
    res: Response,
    httpStatus: number,
    userMessage: string,
    internalCode: string,
    extra?: Record<string, unknown>
) {
    console.log(
        JSON.stringify(
            {
                at: "everlybot",
                event: "client_error",
                rid: (res.locals as any).requestId,
                httpStatus,
                internalCode,
                userMessage,
                ...((extra && typeof extra === "object") ? { extra } : {})
            },
            null,
            2
        )
    );

    res.status(httpStatus).json({ error: userMessage });
}


morgan.token("body_preview", (req: Request) => {
    const msg =
        (req.body?.message && typeof req.body.message === "string") ? req.body.message : "";
    const preview = msg.replace(/\s+/g, " ").trim().slice(0, 120);
    return preview ? JSON.stringify({ messagePreview: preview }) : "";
});

morgan.token("rid", (_req: Request, res: Response) => {
    return (res.locals as any).requestId ?? "";
});

const logsDir = path.join(process.cwd(), "logs");
if (!fs.existsSync(logsDir)) fs.mkdirSync(logsDir, { recursive: true });
const accessLogStream = fs.createWriteStream(path.join(logsDir, "server.log"), { flags: "a" });

app.use(new RequestIdGuard().middleware());

app.use(
    morgan(
        ":remote-addr :method :url :status :res[content-length] - :response-time ms rid=:rid :body_preview"
    )
);

app.use(
    morgan(
        ":remote-addr :method :url :status :res[content-length] - :response-time ms rid=:rid :body_preview",
        { stream: accessLogStream }
    )
);

const guards: Guard[] = [
    new RateLimitGuard(RATE_LIMIT_RPM, RATE_LIMIT_BURST, (req) => `ip:${getClientIp(req)}`),
    new RequestValidationGuard(MAX_MESSAGE_LENGTH),
];

for (const g of guards) {
    app.use(g.middleware());
}

app.post("/api/chat", async (req: Request, res: Response) => {
    const userQuery: string = (req as any).validatedMessage;

    try {
        console.log(
            JSON.stringify(
                {
                    at: "everlybot",
                    event: "incoming",
                    rid: (res.locals as any).requestId,
                    ip: getClientIp(req),
                    messagePreview: userQuery.slice(0, 120),
                },
                null,
                2
            )
        );

        const payload: InboundMessage = {
            content: userQuery,
            context: {},
        };

        const response = await axios.post<SynapSysResponse>(synapsysUrl, payload, {
            timeout: Number(process.env.SYNAPSYS_TIMEOUT_MS || 20000),
            headers: {
                "Content-Type": "application/json",
                "X-SynapSys-Key": SYNAPSYS_KEY,
                "X-SynapSys-Sender": SENDER_ID,
            },
        });

        const data = response.data;

        console.log(
            JSON.stringify(
                {
                    at: "everlybot",
                    event: "synapsys_response",
                    rid: (res.locals as any).requestId,
                    result: "PASS",
                    synapsys: {
                        sender: data.sender,
                        status: data.metadata?.status,
                        reason: data.metadata?.reason ?? null,
                        contentPreview: (data.content || "").substring(0, 250),
                    },
                },
                null,
                2
            )
        );

        res.json(data);
    } catch (error: any) {
        if (axios.isAxiosError(error) && error.response) {
            const status = error.response.status;
            const data = error.response.data as any;

            console.log(
                JSON.stringify(
                    {
                        at: "everlybot",
                        event: "synapsys_error",
                        rid: (res.locals as any).requestId,
                        result: "FAIL",
                        httpStatus: status,
                        synapsys: {
                            sender: data?.sender ?? "unknown",
                            status: data?.metadata?.status ?? "error",
                            reason: data?.metadata?.reason ?? "unknown",
                            contentPreview: (data?.content || "").substring(0, 250),
                        },
                    },
                    null,
                    2
                )
            );

            if (status >= 500) {
                sendClientError(res, 503, "Service unavailable. Please try again.", "SYNAPSYS_5XX", {
                    synapsysStatus: status,
                    synapsysSender: data?.sender,
                    synapsysReason: data?.metadata?.reason
                });
                return;
            }

            res.status(status).json(data);
            return;
        }

        console.log(
            JSON.stringify(
                {
                    at: "everlybot",
                    event: "critical_error",
                    rid: (res.locals as any).requestId,
                    result: "FAIL",
                    httpStatus: 503,
                    message: error?.message || String(error),
                },
                null,
                2
            )
        );

        sendClientError(res, 503, "Service unavailable. Please try again.", "SERVICE_UNAVAILABLE", {
            message: error?.message || String(error)
        });
    }
});

app.get("/health", async (_req: Request, res: Response) => {
    const self = {
        status: "ok",
        pid: process.pid,
        uptime_seconds: Math.floor(process.uptime()),
        cwd: process.cwd(),
    };

    const synapSysResult: any = {
        status: "unknown",
        statusCode: null,
        latencyMs: null,
        error: null,
    };

    try {
        const t0 = Date.now();
        const resp = await axios.get(`${base}/health`, { timeout: 3000 });
        synapSysResult.statusCode = resp.status;
        synapSysResult.latencyMs = Date.now() - t0;
        synapSysResult.status = resp.status >= 200 && resp.status < 300 ? "ok" : "error";
    } catch (err: any) {
        if (axios.isAxiosError(err)) {
            if (err.response) {
                synapSysResult.status = "error";
                synapSysResult.statusCode = err.response.status;
                synapSysResult.error = "synapsys_non_2xx";
            } else {
                synapSysResult.status = "down";
                synapSysResult.error = err.message;
            }
        } else {
            synapSysResult.status = "down";
            synapSysResult.error = String(err);
        }
    }

    res.json({ self, synapSys: synapSysResult });
});

app.use((err: any, _req: Request, res: Response, _next: NextFunction) => {
    console.error(
        JSON.stringify(
            {
                at: "everlybot",
                event: "unhandled_error",
                httpStatus: 500,
                message: err?.message || String(err),
            },
            null,
            2
        )
    );
    sendClientError(res, 500, "An unexpected error occurred. Please try again.", "SERVER_ERROR", {
        message: err?.message || String(err)
    });
});

const server = app.listen(PORT, () => {
    const addr = server.address();
    let listenLocation = "unknown";

    if (typeof addr === "string") {
        listenLocation = addr;
    } else if (addr && typeof addr === "object") {
        const host = addr.address === "::" || addr.address === "0.0.0.0" ? "localhost" : addr.address;
        listenLocation = `${host}:${addr.port}`;
    }

    console.log(`>>> EverlyBot (TS) is online on ${listenLocation}`);
    console.log(`>>> Connected to SynapSys at ${synapsysUrl}`);
});
