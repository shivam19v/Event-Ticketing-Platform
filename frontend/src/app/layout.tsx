import './globals.css';
import type { Metadata } from 'next';
import Providers from '@/components/common/Providers';
import Navbar from '@/components/common/Navbar';

export const metadata: Metadata = {
  title: 'EventSphere',
  description: 'Distributed event management & ticketing platform',
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body>
        <Providers>
          <Navbar />
          <main className="max-w-6xl mx-auto px-4 py-8">{children}</main>
        </Providers>
      </body>
    </html>
  );
}
