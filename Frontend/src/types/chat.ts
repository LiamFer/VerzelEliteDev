export interface Message {
  id: string;
  role: "user" | "assistant" | "error";
  content: string;
  timestamp: Date;
  isThinking?: boolean;
  offer?: SchedulingOffer;
}

export interface SchedulingOffer {
  action: "offer";
  message: string;
  data: SchedulingSlot[];
}

export interface SchedulingSlot {
  scheduling_url: string;
  start_time: string;
  invitees_remaining: number;
  status: "available";
}

export interface AImessage {
  action:string
  message:string
  data:any
}
