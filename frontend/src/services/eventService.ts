import { api } from './api';
import { EventDetail, EventSummary, PageResponse } from '@/types';

export const eventService = {
  async searchEvents(params: { category?: string; city?: string; search?: string; page?: number; size?: number }) {
    const { data } = await api.get<PageResponse<EventSummary>>('/events', { params });
    return data;
  },

  async getEvent(id: string) {
    const { data } = await api.get<EventDetail>(`/events/${id}`);
    return data;
  },

  async getSeatMap(eventId: string) {
    const { data } = await api.get(`/events/${eventId}/seats`);
    return data;
  },
};
