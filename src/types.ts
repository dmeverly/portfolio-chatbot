export interface UserPayload {
    message: string;
}

export interface InboundMessage {
    content: string;
    context: {};
}

export interface SynapSysResponse {
    sender: string;
    content: string;
    metadata: {
        status: string;
        reason?: string;
    };
    context: {};
    timestamp: string;
}