import Link from "next/link";

type Props = {
  title: string;
  code: string;
  percent?: number;
  gradient?: string; // Tailwind classes for the preview band
  logoText?: string;
  href?: string;
};

export default function CourseCard({ title, code, percent = 0, gradient = "from-fuchsia-500 via-purple-500 to-indigo-500", logoText, href }: Props) {
  const Wrapper: React.FC<{ children: React.ReactNode }> = ({ children }) =>
    href ? <Link href={href} className="block">{children}</Link> : <>{children}</>;

  return (
    <div className="rounded-lg border bg-white shadow-sm overflow-hidden hover:shadow transition-shadow">
      <Wrapper>
      <div className={`h-28 bg-gradient-to-r ${gradient}`}>
        {logoText && (
          <div className="h-full w-full flex items-center justify-center">
            <span className="bg-white/90 text-primary text-sm font-semibold px-3 py-1 rounded">
              {logoText}
            </span>
          </div>
        )}
      </div>
      <div className="px-4 py-3 text-sm">
        <div className="flex items-start justify-between gap-2">
          <div>
            <div className="font-medium text-primary leading-snug line-clamp-2">{title}</div>
            <div className="text-[11px] text-primary-400 mt-1">{code}</div>
          </div>
          <button className="text-primary-300 hover:text-primary-500" aria-label="More">
            ⋮
          </button>
        </div>
        <div className="text-[11px] text-primary-400 mt-2">{percent}% complete</div>
      </div>
      </Wrapper>
    </div>
  );
}
