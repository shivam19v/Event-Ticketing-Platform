'use client';
import Link from 'next/link';
import { useEffect, useState } from 'react';
import { authService } from '@/services/authService';
import { User } from '@/types';

export default function Navbar() {
  const [user, setUser] = useState<User | null>(null);

  useEffect(() => {
    setUser(authService.getCurrentUser());
  }, []);

  const handleLogout = async () => {
    await authService.logout();
    setUser(null);
    window.location.href = '/';
  };

  return (
    <nav className="bg-white border-b sticky top-0 z-10">
      <div className="max-w-6xl mx-auto px-4 h-16 flex items-center justify-between">
        <Link href="/" className="text-xl font-bold text-primary">EventSphere</Link>
        <div className="flex items-center gap-4 text-sm">
          <Link href="/" className="hover:text-primary">Browse Events</Link>
          {user ? (
            <>
              <Link href="/bookings" className="hover:text-primary">My Bookings</Link>
              {(user.role === 'ORGANIZER' || user.role === 'ADMIN') && (
                <Link href="/organizer" className="hover:text-primary">Organizer</Link>
              )}
              <span className="text-gray-500">Hi, {user.firstName}</span>
              <button onClick={handleLogout} className="text-red-600 hover:underline">Logout</button>
            </>
          ) : (
            <>
              <Link href="/login" className="hover:text-primary">Login</Link>
              <Link href="/register" className="bg-primary text-white px-4 py-2 rounded-lg hover:bg-primary-dark">
                Sign Up
              </Link>
            </>
          )}
        </div>
      </div>
    </nav>
  );
}
