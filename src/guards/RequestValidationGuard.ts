import { Request, Response, NextFunction } from "express";
import { Guard } from "./Guard";

export class RequestValidationGuard implements Guard {
  public readonly id = "request-validation";

  constructor(private readonly maxMessageChars: number) {}

  middleware() {
    return (req: Request, res: Response, next: NextFunction) => {
      if (req.method !== "POST" || req.path !== "/api/chat") {
        return next();
      }

      const raw: unknown = (req.body as any)?.message;

      if (typeof raw !== "string") {
        res.status(400).json({ error: "Bad request." });
        return;
      }

      const trimmed = raw.trim();

      if (!trimmed) {
        res.status(400).json({ error: "Message cannot be empty." });
        return;
      }

      if (trimmed.length > this.maxMessageChars) {
        res.status(413).json({ error: "Message too long." });
        return;
      }

      (req as any).validatedMessage = trimmed;
      next();
    };
  }
}
