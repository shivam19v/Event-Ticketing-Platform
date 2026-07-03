import { api } from './api';
import { Booking, PageResponse, Reservation } from '@/types';

export const bookingService = {
  async reserve(payload: { eventId: string; ticketTypeId: string; quantity: number; seatIds?: string[]; totalPrice: number }) {
    const { data } = await api.post<Reservation>('/bookings/reserve', payload);
    return data;
  },

  async getBooking(bookingId: string) {
    const { data } = await api.get<Booking>(`/bookings/${bookingId}`);
    return data;
  },

  async confirmBooking(bookingId: string, paymentId: string) {
    const { data } = await api.post<Booking>(`/bookings/${bookingId}/confirm`, { paymentId });
    return data;
  },

  async cancelBooking(bookingId: string) {
    await api.post(`/bookings/${bookingId}/cancel`);
  },

  async getUserBookings(userId: string, page = 0, size = 20) {
    const { data } = await api.get<PageResponse<Booking>>(`/users/${userId}/bookings`, { params: { page, size } });
    return data;
  },
};
