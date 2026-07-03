'use client';
import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { authService } from '@/services/authService';
import { organizerService } from '@/services/organizerService';

export default function OrganizerPage() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const [authorized, setAuthorized] = useState(false);
  const [showForm, setShowForm] = useState(false);
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const [form, setForm] = useState({
    title: '', description: '', category: 'music',
    startTime: '', endTime: '',
    venueName: '', city: '', capacity: 100,
    ticketName: 'General Admission', price: 25, quantity: 100,
  });

  useEffect(() => {
    if (!authService.isAuthenticated()) { router.push('/login'); return; }
    const user = authService.getCurrentUser();
    if (user?.role !== 'ORGANIZER' && user?.role !== 'ADMIN') {
      router.push('/');
      return;
    }
    setAuthorized(true);
  }, [router]);

  const { data } = useQuery({
    queryKey: ['organizer-events'],
    queryFn: () => organizerService.getMyEvents(),
    enabled: authorized,
  });

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      await organizerService.createEvent({
        title: form.title,
        description: form.description,
        category: form.category,
        startTime: new Date(form.startTime).toISOString(),
        endTime: new Date(form.endTime).toISOString(),
        venue: { name: form.venueName, city: form.city, capacity: Number(form.capacity) },
        ticketTypes: [{ name: form.ticketName, price: Number(form.price), quantity: Number(form.quantity) }],
      });
      setShowForm(false);
      queryClient.invalidateQueries({ queryKey: ['organizer-events'] });
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to create event');
    } finally {
      setSubmitting(false);
    }
  };

  const handlePublish = async (eventId: string) => {
    await organizerService.publishEvent(eventId);
    queryClient.invalidateQueries({ queryKey: ['organizer-events'] });
  };

  if (!authorized) return null;

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold">Organizer Dashboard</h1>
        <button onClick={() => setShowForm(!showForm)} className="bg-primary text-white px-4 py-2 rounded-lg">
          {showForm ? 'Cancel' : '+ Create Event'}
        </button>
      </div>

      {showForm && (
        <form onSubmit={handleCreate} className="bg-white border rounded-xl p-6 mb-6 space-y-4">
          {error && <p className="bg-red-50 text-red-600 text-sm p-3 rounded-lg">{error}</p>}
          <div className="grid grid-cols-2 gap-3">
            <input placeholder="Event title" required value={form.title}
              onChange={(e) => setForm({ ...form, title: e.target.value })}
              className="border rounded-lg px-3 py-2 col-span-2" />
            <select value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })}
              className="border rounded-lg px-3 py-2">
              {['music', 'sports', 'conference', 'comedy', 'theater'].map((c) => <option key={c} value={c}>{c}</option>)}
            </select>
            <input placeholder="City" required value={form.city}
              onChange={(e) => setForm({ ...form, city: e.target.value })}
              className="border rounded-lg px-3 py-2" />
            <input placeholder="Venue name" required value={form.venueName}
              onChange={(e) => setForm({ ...form, venueName: e.target.value })}
              className="border rounded-lg px-3 py-2 col-span-2" />
            <input type="datetime-local" required value={form.startTime}
              onChange={(e) => setForm({ ...form, startTime: e.target.value })}
              className="border rounded-lg px-3 py-2" />
            <input type="datetime-local" required value={form.endTime}
              onChange={(e) => setForm({ ...form, endTime: e.target.value })}
              className="border rounded-lg px-3 py-2" />
            <input type="number" placeholder="Ticket price" required value={form.price}
              onChange={(e) => setForm({ ...form, price: Number(e.target.value) })}
              className="border rounded-lg px-3 py-2" />
            <input type="number" placeholder="Quantity" required value={form.quantity}
              onChange={(e) => setForm({ ...form, quantity: Number(e.target.value) })}
              className="border rounded-lg px-3 py-2" />
            <textarea placeholder="Description" value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
              className="border rounded-lg px-3 py-2 col-span-2" rows={3} />
          </div>
          <button type="submit" disabled={submitting} className="bg-primary text-white px-4 py-2 rounded-lg disabled:opacity-50">
            {submitting ? 'Creating...' : 'Create Event (Draft)'}
          </button>
        </form>
      )}

      <div className="space-y-3">
        {data?.content.map((ev) => (
          <div key={ev.id} className="bg-white border rounded-xl p-4 flex items-center justify-between">
            <div>
              <p className="font-medium">{ev.title}</p>
              <p className="text-sm text-gray-500">{ev.status} · {ev.totalAvailable} tickets available</p>
            </div>
            {ev.status === 'DRAFT' && (
              <button onClick={() => handlePublish(ev.id)} className="text-primary hover:underline text-sm">
                Publish
              </button>
            )}
          </div>
        ))}
        {data?.content.length === 0 && <p className="text-gray-500">No events yet. Create your first one above.</p>}
      </div>
    </div>
  );
}
