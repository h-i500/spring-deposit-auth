import clsx from "clsx";

type Variant = "primary" | "secondary" | "danger" | "success";
type Size = "sm" | "md" | "lg";

const base =
  "inline-flex items-center justify-center rounded-lg font-medium transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 disabled:opacity-50 disabled:pointer-events-none";
const sizes: Record<Size, string> = {
  sm: "h-9 px-3 text-sm",
  md: "h-10 px-4 text-sm",
  lg: "h-11 px-5 text-base",
};
const variants: Record<Variant, string> = {
  primary:  "bg-blue-600 text-white hover:bg-blue-700 focus-visible:ring-blue-600",
  secondary:"bg-white text-gray-900 border border-gray-300 hover:bg-gray-50 focus-visible:ring-gray-400",
  danger:   "bg-red-600 text-white hover:bg-red-700 focus-visible:ring-red-600",
  success:  "bg-emerald-600 text-white hover:bg-emerald-700 focus-visible:ring-emerald-600",
};

export function Button({
  children, variant = "primary", size = "md", className, ...props
}: React.ButtonHTMLAttributes<HTMLButtonElement> & {variant?:Variant; size?:Size}) {
  return (
    <button className={clsx(base, sizes[size], variants[variant], className)} {...props}>
      {children}
    </button>
  );
}
