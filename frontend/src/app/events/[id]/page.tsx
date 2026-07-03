'use client';
import { useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { useQuery } from '@tanstack/react-query';
import { eventService } from '@/services/eventService';
import { bookingService } from '@/services/bookingService';
import { paymentService } from '@/services/paymentService';
import { authService } from '@/services/authService';
import { TicketType } from '@/types';
import { format } from 'date-fns';

type Step = 'browse' | 'reserving' | 'paying' | 'done';

export default function EventDetailPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const eventId = params?.id as string;

  const [selectedType, setSelectedType] = useState<TicketType | null>(null);
  const [quantity, setQuantity] = useState(1);
  const [step, setStep] = useState<Step>('browse');
  const [error, setError] = useState('');

  const { data: event, isLoading } = useQuery({
    queryKey: ['event', eventId],
    queryFn: () => eventService.getEvent(eventId),
  });

  const handleReserve = async () => {
    if (!authService.isAuthenticated()) { router.push('/login'); return; }
    if (!selectedType) return;

    setError('');
    setStep('reserving');

    try {
      const totalPrice = Number((selectedType.price * quantity).toFixed(2));

      // 1. Reserve — backend atomically creates both Reservation + Booking rows
      //    and now returns BOTH reservationId AND bookingId.
      const reservation = await bookingService.reserve({
        eventId,
        ticketTypeId: selectedType.id,
        quantity,
        totalPrice,
      });

      setStep('paying');

      // 2. Initiate payment — uses bookingId (NOT reservationId)
      const idempotencyKey = `${reservation.bookingId}-${Date.now()}`;
      const payment = await paymentService.initiatePayment({
        bookingId: reservation.bookingId,
        amount: totalPrice,
        idempotencyKey,
      });

      // 3. Poll payment status (stub mode auto-completes after ~2 s)
      let attempts = 0;
      const maxAttempts = 20;

      const poll = setInterval(async () => {
        attempts++;
        try {
          const status = await paymentService.getStatus(payment.paymentId);

          if (status.status === 'SUCCESS') {
            clearInterval(poll);
            // 4. Confirm booking with the real bookingId
            await bookingService.confirmBooking(reservation.bookingId, payment.paymentId);
            setStep('done');
          } else if (status.status === 'FAILED' || attempts >= maxAttempts) {
            clearInterval(poll);
            setError('Payment failed or timed out. Please try again.');
            setStep('browse');
          }
        } catch {
          clearInterval(poll);
          setError('Could not verify payment status. Check your bookings.');
          setStep('browse');
        }
      }, 1500);

    } catch (err: any) {
      setError(
        err?.response?.data?.message ||
        'Reservation failed — the seats may no longer be available.'
      );
      setStep('browse');
    }
  };

  if (isLoading) return <p className="text-gray-500 py-12 text-center">Loading event…</p>;
  if (!event) return <p className="text-red-500 py-12 text-center">Event not found.</p>;

  return (
    <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
      {/* ── Left: event info ──────────────────────────────────────────── */}
      <div className="lg:col-span-2">
        <div className="h-64 bg-gradient-to-br from-primary to-primary-light rounded-xl mb-6 overflow-hidden flex items-center justify-center text-white text-6xl font-bold">
          {event.imageUrl ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img src={event.imageUrl} alt={event.title} className="w-full h-full object-cover" />
          ) : (
            event.title.charAt(0)
          )}
        </div>

        <span className="text-xs font-semibold uppercase tracking-wider text-primary">
          {event.category}
        </span>
        <h1 className="text-3xl font-bold mt-1">{event.title}</h1>
        <p className="text-gray-500 mt-2">
          {format(new Date(event.startTime), 'EEEE, MMMM d, yyyy · h:mm a')}
          {' — '}
          {format(new Date(event.endTime), 'h:mm a')}
        </p>
        {event.venue && (
          <p className="text-gray-500">
            {event.venue.name}, {event.venue.city}
            {event.venue.state ? `, ${event.venue.state}` : ''}
          </p>
        )}

        {event.description && (
          <div className="mt-6">
            <h2 className="font-semibold text-lg mb-2">About this event</h2>
            <p className="text-gray-700 whitespace-pre-line">{event.description}</p>
          </div>
        )}

        {/* Venue detail */}
        {event.venue && (
          <div className="mt-6 p-4 bg-gray-50 rounded-xl">
            <h2 className="font-semibold mb-1">Venue</h2>
            <p className="text-sm text-gray-600">{event.venue.name}</p>
            {event.venue.address && <p className="text-sm text-gray-500">{event.venue.address}</p>}
            <p className="text-sm text-gray-500">
              {[event.venue.city, event.venue.state, event.venue.country].filter(Boolean).join(', ')}
            </p>
            <p className="text-sm text-gray-400 mt-1">Capacity: {event.venue.capacity.toLocaleString()}</p>
          </div>
        )}
      </div>

      {/* ── Right: ticket selector ────────────────────────────────────── */}
      <div className="bg-white border rounded-xl p-6 h-fit sticky top-20 shadow-sm">
        <h2 className="font-semibold text-lg mb-4">Get Tickets</h2>

        {step === 'browse' && (
          <>
            <div className="space-y-3 mb-5">
              {event.ticketTypes.map((t) => {
                const isSoldOut = t.available <= 0;
                const isSelected = selectedType?.id === t.id;
                return (
                  <label
                    key={t.id}
                    className={`flex items-start justify-between border rounded-lg p-3 cursor-pointer transition
                      ${isSoldOut ? 'opacity-40 cursor-not-allowed' : ''}
                      ${isSelected ? 'border-primary ring-1 ring-primary bg-primary/5' : 'hover:border-gray-400'}`}
                  >
                    <div className="flex items-start gap-2">
                      <input
                        type="radio"
                        name="ticketType"
                        disabled={isSoldOut}
                        checked={isSelected}
                        onChange={() => { if (!isSoldOut) { setSelectedType(t); setQuantity(1); } }}
                        className="mt-0.5"
                      />
                      <div>
                        <p className="font-medium text-sm">{t.name}</p>
                        {t.description && (
                          <p className="text-xs text-gray-400 mt-0.5">{t.description}</p>
                        )}
                        <p className="text-xs text-gray-400 mt-0.5">
                          {isSoldOut ? 'Sold out' : `${t.available} left`}
                        </p>
                      </div>
                    </div>
                    <span className="font-bold text-sm shrink-0 ml-2">
                      {t.price === 0 ? 'Free' : `$${t.price}`}
                    </span>
                  </label>
                );
              })}
            </div>

            {selectedType && (
              <div className="flex items-center gap-3 mb-5">
                <label className="text-sm text-gray-600 shrink-0">Qty</label>
                <select
                  value={quantity}
                  onChange={(e) => setQuantity(Number(e.target.value))}
                  className="border rounded-lg px-2 py-1 text-sm flex-1"
                >
                  {Array.from(
                    { length: Math.min(10, selectedType.available) },
                    (_, i) => i + 1
                  ).map((n) => (
                    <option key={n} value={n}>{n}</option>
                  ))}
                </select>
              </div>
            )}

            {error && (
              <p className="bg-red-50 text-red-600 text-sm p-3 rounded-lg mb-3">{error}</p>
            )}

            {selectedType && (
              <div className="flex justify-between text-sm text-gray-600 mb-3">
                <span>{quantity} × ${selectedType.price}</span>
                <span className="font-semibold">${(selectedType.price * quantity).toFixed(2)}</span>
              </div>
            )}

            <button
              onClick={handleReserve}
              disabled={!selectedType}
              className="w-full bg-primary text-white py-2.5 rounded-lg font-medium
                         hover:bg-primary-dark disabled:opacity-40 disabled:cursor-not-allowed transition"
            >
              {selectedType
                ? `Reserve · $${(selectedType.price * quantity).toFixed(2)}`
                : 'Select a ticket type'}
            </button>

            <p className="text-xs text-gray-400 text-center mt-2">
              Your reservation holds for 15 minutes
            </p>
          </>
        )}

        {step === 'reserving' && (
          <div className="text-center py-8">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary mx-auto mb-3" />
            <p className="text-gray-500 text-sm">Securing your seats…</p>
          </div>
        )}

        {step === 'paying' && (
          <div className="text-center py-8">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary mx-auto mb-3" />
            <p className="text-gray-500 text-sm">Processing payment…</p>
            <p className="text-xs text-gray-400 mt-1">Please don&apos;t close this page</p>
          </div>
        )}

        {step === 'done' && (
          <div className="text-center py-8">
            <div className="text-4xl mb-3">🎉</div>
            <p className="font-semibold text-green-700 mb-1">Booking confirmed!</p>
            <p className="text-sm text-gray-500 mb-4">
              Your tickets are ready. Check your email for confirmation.
            </p>
            <a
              href="/bookings"
              className="inline-block bg-primary text-white text-sm px-4 py-2 rounded-lg hover:bg-primary-dark"
            >
              View my bookings →
            </a>
          </div>
        )}
      </div>
    </div>
  );
}
