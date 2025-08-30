import { PropsWithChildren } from "react";

export default function Card({ title, children }: PropsWithChildren<{ title?: string }>) {
  return (
    <section className="card">
      {title && <h2>{title}</h2>}
      {children}
    </section>
  );
}
