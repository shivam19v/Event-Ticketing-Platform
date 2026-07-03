import Link from 'next/link';
import { EventSummary } from '@/types';
import { format } from 'date-fns';

export default function EventCard({ event }: { event: EventSummary }) {
  return (
    <Link href={`/events/${event.id}`} className="block bg-white rounded-xl shadow-sm hover:shadow-md transition-shadow overflow-hidden border">
      <div className="h-40 bg-gradient-to-br from-primary to-primary-light flex items-center justify-center text-white text-3xl font-bold">
        {event.imageUrl ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img src={event.imageUrl} alt={event.title} className="w-full h-full object-cover" />
        ) : (
          event.title.charAt(0)
        )}
      </div>
      <div className="p-4">
        <span className="text-xs font-medium text-primary uppercase">{event.category}</span>
        <h3 className="font-semibold text-lg mt-1 line-clamp-1">{event.title}</h3>
        <p className="text-sm text-gray-500 mt-1">
          {format(new Date(event.startTime), 'MMM d, yyyy · h:mm a')}
        </p>
        {event.venueName && <p className="text-sm text-gray-500">{event.venueName}, {event.city}</p>}
        <div className="flex items-center justify-between mt-3">
          <span className="font-bold text-primary">
            {event.lowestPrice != null ? `From $${event.lowestPrice}` : 'Free'}
          </span>
          <span className="text-xs text-gray-400">{event.totalAvailable} tickets left</span>
        </div>
      </div>
    </Link>
  );
}
