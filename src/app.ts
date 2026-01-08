import dotenv from 'dotenv';
dotenv.config(); 

import express, { Request, Response } from 'express';
import morgan from 'morgan';
import fs from 'fs';
import path from 'path';
import cors from 'cors';
import axios from 'axios';
import { InboundMessage, SynapSysResponse } from './types';
import { loadEverlybotPrivateConfig } from "./config/privateConfig";

const privateConfig = loadEverlybotPrivateConfig();

const SENDER_ID = privateConfig.senderId;
const SYNAPSYS_KEY = privateConfig.synapsysClientKey;

const app = express();
const PORT = process.env.PORT || 3000;

const base = process.env.SYNAPSYS_BASE_URL!;
if (!base) {
    throw new Error("Missing env var: SYNAPSYS_BASE_URL");
}
const synapsysUrl = `${base}/api/v1/chat`;

app.use(express.json());
app.use(cors({
    origin: process.env.ALLOWED_ORIGIN || '*',
    methods: ['POST', 'OPTIONS']
}));

morgan.token('body', (req: Request) => {
    try { return JSON.stringify(req.body); } catch { return ''; }
});

const logsDir = path.join(process.cwd(), 'logs');
if (!fs.existsSync(logsDir)) fs.mkdirSync(logsDir, { recursive: true });
const accessLogStream = fs.createWriteStream(path.join(logsDir, 'server.log'), { flags: 'a' });

app.use(morgan(':remote-addr :method :url :status :res[content-length] - :response-time ms :body'));
app.use(morgan(':remote-addr :method :url :status :res[content-length] - :response-time ms :body', { stream: accessLogStream }));

app.post('/api/chat', async (req: Request, res: Response) => {
    try {
        const userQuery: string = req.body.message;

        if (!userQuery || userQuery.trim().length === 0) {
            res.status(400).json({ error: "Message cannot be empty." });
            return;
        }

        console.log(`[EverlyBot] Incoming Query: "${userQuery.substring(0, 50)}..."`);
        console.log("[EverlyBot] synapsysUrl =", synapsysUrl);

        const payload: InboundMessage = {
            content: userQuery,
            context: {}
        };

        const response = await axios.post<SynapSysResponse>(
            synapsysUrl,
            payload,
            {
                headers: {
                    "Content-Type": "application/json",
                    "X-SynapSys-Key": SYNAPSYS_KEY,
                    "X-SynapSys-Sender": SENDER_ID,
                }
            }
        );

        const data = response.data;

        const logEntry = {
            result: "PASS",
            timestamp: new Date().toISOString(),
            userQuery: userQuery.substring(0, 100),
            synapsys: {
                sender: data.sender,
                status: data.metadata.status,
                reason: data.metadata.reason || null,
                contentPreview: (data.content || "").substring(0, 250)
            }
        };

        console.log(JSON.stringify(logEntry, null, 2));
        res.json(data);

    } catch (error: any) {
        if (axios.isAxiosError(error) && error.response) {
            const status = error.response.status;
            const data = error.response.data as any;

            const logEntry = {
                result: "FAIL",
                timestamp: new Date().toISOString(),
                userQuery: req.body.message?.substring(0, 100) || "unknown",
                error: {
                    httpStatus: status,
                    synapsys: {
                        sender: data.sender || "unknown",
                        status: data.metadata?.status || "error",
                        reason: data.metadata?.reason || "unknown",
                        contentPreview: (data.content || "").substring(0, 250)
                    }
                }
            };

            console.log(JSON.stringify(logEntry, null, 2));
            res.status(status).json(data);
        } else {
            const logEntry = {
                result: "FAIL",
                timestamp: new Date().toISOString(),
                userQuery: req.body.message?.substring(0, 100) || "unknown",
                error: {
                    httpStatus: 500,
                    type: "critical",
                    message: error?.message || String(error)
                }
            };

            console.log(JSON.stringify(logEntry, null, 2));
            res.status(500).json({ error: "Service unavailable. Please try again later." });
        }
    }
});

const server = app.listen(PORT, () => {
    const addr = server.address();
    let listenLocation = 'unknown';
    if (typeof addr === 'string') {
        listenLocation = addr;
    } else if (addr && typeof addr === 'object') {
        const host = (addr.address === '::' || addr.address === '0.0.0.0') ? 'localhost' : addr.address;
        listenLocation = `${host}:${addr.port}`;
    }

    console.log(`>>> EverlyBot (TS) is online on ${listenLocation}`);
    console.log(`>>> Connected to SynapSys at ${synapsysUrl}`);
});

app.get('/health', async (_req: Request, res: Response) => {
    const self = {
        status: 'ok',
        pid: process.pid,
        uptime_seconds: Math.floor(process.uptime()),
        cwd: process.cwd()
    };

    const synapSysResult: any = {
        status: 'unknown',
        statusCode: null,
        latencyMs: null,
        error: null
    };

    if (!synapsysUrl) {
        synapSysResult.status = 'unknown';
        synapSysResult.error = 'SYNAPSYS_BASE_URL not configured';
    } else {
        try {
            const t0 = Date.now();
            const resp = await axios.get(`${base}/health`, { timeout: 3000 });
            synapSysResult.statusCode = resp.status;
            synapSysResult.latencyMs = Date.now() - t0;
            synapSysResult.status = (resp.status >= 200 && resp.status < 300) ? 'ok' : 'error';
        } catch (err: any) {
            synapSysResult.latencyMs = null;
            if (axios.isAxiosError(err)) {
                if (err.response) {
                    synapSysResult.status = 'error';
                    synapSysResult.statusCode = err.response.status;
                    synapSysResult.error = err.response.data;
                } else {
                    synapSysResult.status = 'down';
                    synapSysResult.error = err.message;
                }
            } else {
                synapSysResult.status = 'down';
                synapSysResult.error = String(err);
            }
        }
    }

    res.json({ self, synapSys: synapSysResult });
});
