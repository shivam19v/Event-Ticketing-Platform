'use client';
import { useState } from 'react';
import { authService } from '@/services/authService';

export default function RegisterPage() {
  const [form, setForm] = useState({ email: '', password: '', firstName: '', lastName: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await authService.register(form);
      // Hard redirect so root layout re-mounts and Navbar re-reads localStorage
      window.location.href = '/';
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Registration failed');
      setLoading(false);
    }
  };

  return (
    <div className="max-w-md mx-auto bg-white p-8 rounded-xl shadow-sm border mt-12">
      <h1 className="text-2xl font-bold mb-6">Create your account</h1>
      {error && <p className="bg-red-50 text-red-600 text-sm p-3 rounded-lg mb-4">{error}</p>}
      <form onSubmit={handleSubmit} className="space-y-4">
        <div className="grid grid-cols-2 gap-3">
          <input placeholder="First name" required value={form.firstName}
            onChange={(e) => setForm({ ...form, firstName: e.target.value })}
            className="border rounded-lg px-3 py-2" />
          <input placeholder="Last name" required value={form.lastName}
            onChange={(e) => setForm({ ...form, lastName: e.target.value })}
            className="border rounded-lg px-3 py-2" />
        </div>
        <input type="email" placeholder="Email" required value={form.email}
          onChange={(e) => setForm({ ...form, email: e.target.value })}
          className="w-full border rounded-lg px-3 py-2" />
        <input type="password" placeholder="Password (min 8 chars)" required minLength={8}
          value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })}
          className="w-full border rounded-lg px-3 py-2" />
        <button type="submit" disabled={loading}
          className="w-full bg-primary text-white py-2 rounded-lg hover:bg-primary-dark disabled:opacity-50">
          {loading ? 'Creating account…' : 'Sign Up'}
        </button>
      </form>
      <p className="text-sm text-gray-500 mt-4 text-center">
        Already have an account? <a href="/login" className="text-primary hover:underline">Sign in</a>
      </p>
    </div>
  );
}
