export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: 'ADMIN' | 'ORGANIZER' | 'ATTENDEE' | 'STAFF';
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  user: User;
}

export interface TicketType {
  id: string;
  name: string;
  description?: string;
  price: number;
  quantity: number;
  sold: number;
  available: number;
}

export interface Venue {
  id?: string;
  name: string;
  address?: string;
  city: string;
  state?: string;
  country?: string;
  capacity: number;
}

export interface EventSummary {
  id: string;
  title: string;
  category: string;
  imageUrl?: string;
  status: string;
  startTime: string;
  city?: string;
  venueName?: string;
  lowestPrice?: number;
  totalAvailable?: number;
}

export interface EventDetail {
  id: string;
  organizerId: string;
  title: string;
  description?: string;
  category: string;
  imageUrl?: string;
  status: string;
  startTime: string;
  endTime: string;
  venue: Venue;
  ticketTypes: TicketType[];
  totalCapacity: number;
  totalSold: number;
}

export interface Reservation {
  reservationId: string;
  bookingId: string;       // ← always returned alongside reservationId
  eventId: string;
  ticketTypeId: string;
  quantity: number;
  totalPrice: number;
  status: string;
  expiresAt: string;
}

export interface Booking {
  id: string;
  reservationId: string;
  eventId: string;
  ticketTypeId: string;
  quantity: number;
  totalPrice: number;
  bookingStatus: string;
  paymentId?: string;
  createdAt: string;
  completedAt?: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
}
