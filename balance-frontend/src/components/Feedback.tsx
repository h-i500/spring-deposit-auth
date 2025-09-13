export function Spinner() {
  return (
    <div className="inline-flex items-center gap-2 text-gray-600 text-sm" role="status" aria-live="polite">
      <svg className="size-4 animate-spin" viewBox="0 0 24 24" fill="none">
        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"/>
        <path className="opacity-75" d="M4 12a8 8 0 018-8" stroke="currentColor" strokeWidth="4" strokeLinecap="round"/>
      </svg>
      Loading...
    </div>
  );
}

export function AlertError({ children }: { children: React.ReactNode }) {
  return (
    <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-red-800 text-sm">
      {children}
    </div>
  );
}

export function Success({ children }: { children: React.ReactNode }) {
  return (
    <div className="inline-flex items-center gap-2 rounded-md border border-emerald-200 bg-emerald-50 px-3 py-1.5 text-emerald-800 text-sm">
      <svg className="size-4" viewBox="0 0 24 24" fill="none">
        <path d="M20 6L9 17l-5-5" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
      </svg>
      {children}
    </div>
  );
}
