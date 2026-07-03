import { api } from './api';
import { EventSummary, PageResponse } from '@/types';

export interface CreateEventPayload {
  title: string;
  description?: string;
  category: string;
  startTime: string;
  endTime: string;
  venue: { name: string; city: string; capacity: number };
  ticketTypes: { name: string; price: number; quantity: number }[];
}

export const organizerService = {
  async getMyEvents(page = 0, size = 20) {
    const { data } = await api.get<PageResponse<EventSummary>>('/events/my', { params: { page, size } });
    return data;
  },

  async createEvent(payload: CreateEventPayload) {
    const { data } = await api.post('/events', payload);
    return data;
  },

  async publishEvent(eventId: string) {
    const { data } = await api.post(`/events/${eventId}/publish`);
    return data;
  },
};
