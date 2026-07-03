'use client';
import { useEffect, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useRouter } from 'next/navigation';
import { authService } from '@/services/authService';
import { bookingService } from '@/services/bookingService';
import { format } from 'date-fns';

const STATUS_COLORS: Record<string, string> = {
  CONFIRMED: 'bg-green-100 text-green-700',
  AWAITING_PAYMENT: 'bg-yellow-100 text-yellow-700',
  CANCELLED: 'bg-red-100 text-red-700',
  EXPIRED: 'bg-gray-100 text-gray-700',
};

export default function BookingsPage() {
  const router = useRouter();
  const [userId, setUserId] = useState<string | null>(null);

  useEffect(() => {
    if (!authService.isAuthenticated()) {
      router.push('/login');
      return;
    }
    const user = authService.getCurrentUser();
    setUserId(user?.id ?? null);
  }, [router]);

  const { data, isLoading } = useQuery({
    queryKey: ['bookings', userId],
    queryFn: () => bookingService.getUserBookings(userId as string),
    enabled: !!userId,
  });

  if (!userId) return null;

  return (
    <div>
      <h1 className="text-2xl font-bold mb-6">My Bookings</h1>
      {isLoading && <p className="text-gray-500">Loading bookings...</p>}

      {data && data.content.length === 0 && (
        <p className="text-gray-500">You haven&apos;t booked any events yet. <a href="/" className="text-primary hover:underline">Browse events</a></p>
      )}

      <div className="space-y-4">
        {data?.content.map((b) => (
          <div key={b.id} className="bg-white border rounded-xl p-4 flex items-center justify-between">
            <div>
              <p className="font-medium">Booking #{b.id.slice(0, 8)}</p>
              <p className="text-sm text-gray-500">
                Qty: {b.quantity} · ${b.totalPrice} · {format(new Date(b.createdAt), 'MMM d, yyyy')}
              </p>
            </div>
            <span className={`text-xs font-medium px-3 py-1 rounded-full ${STATUS_COLORS[b.bookingStatus] || 'bg-gray-100'}`}>
              {b.bookingStatus.replace('_', ' ')}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}
