import fs from "node:fs";
import path from "node:path";

export type EverlybotPrivateConfig = {
  senderId: string;
  synapsysClientKey: string;
  providerId: string;
  model: string;
  systemInstructionPath: string;
  fileSearchStoreName: string;
};

function requiredEnv(name: string): string {
  const v = process.env[name];
  if (!v || !v.trim()) throw new Error(`Missing env var: ${name}`);
  return v.trim();
}

function requiredString(obj: Record<string, unknown>, key: string): string {
  const v = obj[key];
  if (typeof v !== "string" || !v.trim()) {
    throw new Error(`Missing/invalid config field: ${key}`);
  }
  return v.trim();
}

export function loadEverlybotPrivateConfig(): EverlybotPrivateConfig {
  const secretsPath = requiredEnv("SECRETS_PATH");
  const filename = (process.env.EVERLYBOT_CONFIG_FILE || "everlybot.json").trim();

  const fullPath = path.resolve(secretsPath, filename);
  if (!fs.existsSync(fullPath)) throw new Error(`Config file not found: ${fullPath}`);

  const raw = fs.readFileSync(fullPath, "utf-8");
  let parsed: unknown;
  try {
    parsed = JSON.parse(raw);
  } catch {
    throw new Error(`Invalid JSON in config file: ${fullPath}`);
  }

  if (!parsed || typeof parsed !== "object") {
    throw new Error(`Config JSON must be an object: ${fullPath}`);
  }

  const obj = parsed as Record<string, unknown>;

  return {
    senderId: requiredString(obj, "senderId"),
    synapsysClientKey: requiredString(obj, "synapsysClientKey"),
    providerId: requiredString(obj, "providerId"),
    model: requiredString(obj, "model"),
    systemInstructionPath: requiredString(obj, "systemInstructionPath"),
    fileSearchStoreName: requiredString(obj, "fileSearchStoreName"),
  };
}
