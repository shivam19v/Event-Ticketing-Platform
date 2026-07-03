import { api } from './api';

export const paymentService = {
  async initiatePayment(payload: { bookingId: string; amount: number; idempotencyKey: string }) {
    const { data } = await api.post('/payments/initiate', payload);
    return data;
  },

  async getStatus(paymentId: string) {
    const { data } = await api.get(`/payments/${paymentId}/status`);
    return data;
  },
};
