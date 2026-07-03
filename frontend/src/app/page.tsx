'use client';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { eventService } from '@/services/eventService';
import EventCard from '@/components/events/EventCard';

const CATEGORIES = ['music', 'sports', 'conference', 'comedy', 'theater'];

export default function HomePage() {
  const [category, setCategory] = useState<string | undefined>();
  const [search, setSearch] = useState('');

  const { data, isLoading, error } = useQuery({
    queryKey: ['events', category, search],
    queryFn: () => eventService.searchEvents({ category, search: search || undefined, size: 24 }),
  });

  return (
    <div>
      <div className="mb-8">
        <h1 className="text-3xl font-bold mb-2">Discover Events</h1>
        <p className="text-gray-500">Find concerts, conferences, and experiences near you.</p>
      </div>

      <div className="flex flex-wrap gap-2 mb-6">
        <input
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Search events..."
          className="border rounded-lg px-4 py-2 flex-1 min-w-[200px]"
        />
        <button
          onClick={() => setCategory(undefined)}
          className={`px-4 py-2 rounded-lg text-sm ${!category ? 'bg-primary text-white' : 'bg-white border'}`}
        >
          All
        </button>
        {CATEGORIES.map((c) => (
          <button
            key={c}
            onClick={() => setCategory(c)}
            className={`px-4 py-2 rounded-lg text-sm capitalize ${category === c ? 'bg-primary text-white' : 'bg-white border'}`}
          >
            {c}
          </button>
        ))}
      </div>

      {isLoading && <p className="text-gray-500">Loading events...</p>}
      {error && <p className="text-red-500">Failed to load events. Is the backend running?</p>}

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
        {data?.content.map((event) => (
          <EventCard key={event.id} event={event} />
        ))}
      </div>

      {data && data.content.length === 0 && (
        <p className="text-gray-500 text-center py-12">No events found. Try a different search.</p>
      )}
    </div>
  );
}
